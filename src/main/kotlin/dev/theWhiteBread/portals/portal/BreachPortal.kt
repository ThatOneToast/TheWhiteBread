package dev.theWhiteBread.portals.portal

import dev.theWhiteBread.colorMul
import dev.theWhiteBread.intToColor
import dev.theWhiteBread.lerpColor
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.portals.PortalType
import dev.theWhiteBread.portals.portal.UnstableBreachPortal
import dev.theWhiteBread.serializables.LocationSerializer
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
open class BreachPortal(
    override val id: String,
    open val owner: String,
    @Serializable(with = LocationSerializer::class)
    override val location: Location,
    @Serializable(with = LocationSerializer::class)
    override val destination: Location?,
    var radius: Double = 1.5,
    val allowed: MutableSet<String> = mutableSetOf(),
    val visibleTo: MutableSet<String> = mutableSetOf(),
    override val portalType: PortalType = PortalType.STABLE,
    override var persistence: Boolean = true,
    var color: Int = 0x50A0FF
) : Portal {

    fun makeUnstable(): UnstableBreachPortal {
        PortalManager.deRegisterPortal(id, true)
        return UnstableBreachPortal(
            id,
            owner,
            location,
            destination,
            persistence = persistence,
            color = color
        ).registerPortal(true)
    }

    override fun renderPortal() {
        val world = location.world ?: return
        val now = System.currentTimeMillis()
        val t = (now % 10_000L) / 1000.0

        // --- Tunables (adjust for look/perf) ----------------------------------
        val viewersRadius = 48                                // receivers radius
        val baseScale = 1.2
        val verticalScale = 1.35
        val depthFront = 1.1                                  // protrusion toward viewer
        val depthBack = 2.6                                   // depth behind portal
        val depthLayers = 6
        val densityFactor = 8.5                               // particles per block-area
        val jitter = 0.08
        val moverChance = 9                                   // spawn mover ~1/moverChance
        val moverBaseSpeed = 0.018                            // suction base speed
        val teethCount = 10
        val toothLen = 4  // colors: dark center -> mid -> outer
        val base = intToColor(color)
        val darkCenter = colorMul(base, 0.08)      // very dark center
        val midColor = lerpColor(darkCenter, base, 0.45)
        val outerColor = lerpColor(base, Color.fromRGB(255, 255, 255), 0.25)
        // ----------------------------------------------------------------------

        // builders (clone per spawn)
        val dustBase = Particle.DUST.builder().count(1)
        val moverBuilder = Particle.DUST.builder().count(0) // count=0 uses offset as dir

        // small unique phase so multiple portals don't pulse identically
        val uniquePhase = (abs(id.hashCode().toLong()) % 1000) / 1000.0
        val mouthPulse = 0.5 + 0.5 * sin(t * 1.9 + uniquePhase * 2.0 * PI) // 0..1
        val scalePulse = 1.0 + 0.22 * sin(t * 2.5 + uniquePhase * 2.0 * PI)

        // scaled radii
        val rx = radius * baseScale * scalePulse
        val ry = rx * verticalScale

        // LOD by distance to nearest player in same world (keeps perf sane)
        val nearest = Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.world == world }
            .minByOrNull { it.location.distanceSquared(location) }
        val distToNearest = nearest?.location?.distance(location) ?: Double.POSITIVE_INFINITY
        val lodFactor = when {
            distToNearest == Double.POSITIVE_INFINITY -> 1.0
            distToNearest > 80.0 -> 0.45
            distToNearest > 50.0 -> 0.75
            else -> 1.0
        }

        // number of random samples per layer (clamped)
        val area = Math.PI * rx * ry
        val perLayer = (area * densityFactor * lodFactor).roundToInt()
            .coerceIn(40, 1600)

        // plane basis (vertical portal plane) - portal faces nearest viewer where possible
        var normal = Vector(0.0, 0.0, 1.0)
        if (nearest != null &&
            nearest.location.distanceSquared(location) <= viewersRadius.toDouble().pow(2)
        ) {
            val v = nearest.eyeLocation.toVector().subtract(location.toVector()).also { it.y = 0.0 }
            if (v.lengthSquared() > 1e-6) normal = v.normalize()
        } else if (location.direction.lengthSquared() > 1e-6) {
            normal = location.direction.clone().also { it.y = 0.0 }.normalize()
        }

        var right = normal.clone().crossProduct(Vector(0.0, 1.0, 0.0))
        if (right.lengthSquared() < 1e-6) right = Vector(1.0, 0.0, 0.0)
        right.normalize()
        val upPlane = right.clone().crossProduct(normal).normalize()

        val rnd = Random.Default

        // --- Filled, pulsing oval with front lip (no visible rings) ----------
        for (layer in 0 until depthLayers) {
            val layerT = if (depthLayers == 1) 0.5 else layer.toDouble() / (depthLayers - 1)
            // map layers from front (positive) -> back (negative)
            val depthOffset = depthFront * (1.0 - layerT) - depthBack * layerT
            // front layers expand a bit to form a lip
            val lipExpand = 1.0 + 0.28 * (1.0 - layerT) * mouthPulse
            val ringRx = rx * lipExpand
            val ringRy = ry * lipExpand

            for (i in 0 until perLayer) {
                // uniform sample inside an ellipse (disk mapped to ellipse)
                val u = rnd.nextDouble()
                val rFrac = sqrt(u)
                val theta = rnd.nextDouble() * 2.0 * PI
                var px = cos(theta) * rFrac * ringRx
                var py = sin(theta) * rFrac * ringRy
                // jitter to avoid patterning
                px += (rnd.nextDouble() - 0.5) * jitter * rx
                py += (rnd.nextDouble() - 0.5) * jitter * ry

                val spawn = Location(
                    world,
                    location.x + right.x * px + upPlane.x * py + normal.x * depthOffset,
                    location.y + right.y * px + upPlane.y * py + normal.y * depthOffset,
                    location.z + right.z * px + upPlane.z * py + normal.z * depthOffset
                )

                // color: deeper => darker; radial => lighter to the rim
                val depthMixColor = lerpColor(darkCenter, midColor, layerT)
                val finalColor = lerpColor(depthMixColor, outerColor, rFrac)
                val size = ((1.15 - 0.85 * rFrac) * (1.0 - layerT * 0.35)).toFloat()

                dustBase.clone()
                    .data(Particle.DustOptions(finalColor, size))
                    .location(spawn) // MUST set location before receivers()
                    .receivers(viewersRadius, true)
                    .spawn()

                // sparse inward movers for a noticeable suction (but controlled)
                if (rnd.nextInt(moverChance) == 0 && rFrac > 0.28 && layerT > 0.22) {
                    val dx = location.x - spawn.x
                    val dy = location.y - spawn.y
                    val dz = location.z - spawn.z
                    val dist2 = dx * dx + dy * dy + dz * dz
                    if (dist2 > 1e-6) {
                        val inv = 1.0 / sqrt(dist2)
                        val dirX = dx * inv
                        val dirY = dy * inv
                        val dirZ = dz * inv
                        // stronger suction when mouthPulse is larger
                        val speed = moverBaseSpeed + mouthPulse * 0.018
                        moverBuilder.clone()
                            .data(Particle.DustOptions(finalColor, size * 0.7f))
                            .location(spawn)
                            .offset(dirX, dirY, dirZ) // direction toward center
                            .extra(speed)
                            .receivers(viewersRadius, true)
                            .spawn()
                    }
                }
            }
        }

        // --- Teeth: inward spikes from rim (gives bite / maw look) ----------
        for (i in 0 until teethCount) {
            val ang = 2.0 * PI * i / teethCount + t * 0.35
            val rimX = cos(ang) * rx * (1.06 + 0.2 * mouthPulse)
            val rimY = sin(ang) * ry * (1.06 + 0.2 * mouthPulse)

            val rimXw = location.x + right.x * rimX + upPlane.x * rimY
            val rimYw = location.y + right.y * rimX + upPlane.y * rimY
            val rimZw = location.z + right.z * rimX + upPlane.z * rimY

            for (s in 1..toothLen) {
                val frac = s.toDouble() / (toothLen + 1)
                // lerp rim -> center (small jitter)
                val px = rimXw * (1.0 - frac) + location.x * frac +
                        (rnd.nextDouble() - 0.5) * 0.06
                val py = rimYw * (1.0 - frac) + location.y * frac +
                        (rnd.nextDouble() - 0.5) * 0.06
                val pz = rimZw * (1.0 - frac) + location.z * frac +
                        (rnd.nextDouble() - 0.5) * 0.06
                val pos = Location(world, px, py, pz)

                val c = lerpColor(darkCenter, midColor, frac * 0.38)
                val size = (0.95f - 0.13f * s)
                dustBase.clone()
                    .data(Particle.DustOptions(c, size))
                    .location(pos)
                    .receivers(viewersRadius, true)
                    .spawn()
            }
        }

        val rimSmoothCount = (64 * lodFactor).roundToInt().coerceIn(24, 120)
        val rimBandSamples = 4
        val rimBandWidth = rx * 0.12                    // how far the band extends (ellipse units)
        val rimDepthJitter = 0.06 * depthBack
        val rimOuterSize = 1.6f
        val rimInnerSize = 0.7f

        for (i in 0 until rimSmoothCount) {
            val ang = 2.0 * Math.PI * i / rimSmoothCount + t * 0.28

            for (sIdx in 0 until rimBandSamples) {
                val s = sIdx.toDouble() / (rimBandSamples - 1)
                // offset from rim toward inside/outside
                val offset = (s - 0.5) * rimBandWidth *
                        (1.0 + 0.08 * sin(t * 1.1 + i.toDouble()))
                val px = cos(ang) * (rx + offset)
                val py = sin(ang) * (ry + offset)

                // slight depth variation so rim isn't perfectly flat
                val depthJ = -(depthFront * 0.12 + depthBack * 0.08) * (1.0 - s) +
                        (Random.Default.nextDouble() - 0.5) * rimDepthJitter

                val spawn = Location(
                    world,
                    location.x + right.x * px + upPlane.x * py + normal.x * depthJ,
                    location.y + right.y * px + upPlane.y * py + normal.y * depthJ,
                    location.z + right.z * px + upPlane.z * py + normal.z * depthJ
                )

                // color slightly darker on the inner side, lighter on the outer side
                val depthMixColor = lerpColor(midColor, darkCenter, 0.12 + 0.85 * (1.0 - s))
                val finalColor = lerpColor(depthMixColor, outerColor, s)
                val size = rimOuterSize - (rimOuterSize - rimInnerSize) * s.toFloat()

                dustBase.clone()
                    .data(Particle.DustOptions(finalColor, size))
                    .location(spawn)              // location BEFORE receivers()
                    .receivers(viewersRadius, true)
                    .spawn()
            }
        }

        // --- Big dark gulp on strong pulse (visual accent) -------------------
        if (sin(t * 1.9 + uniquePhase * 2.0 * PI) > 0.78) {
            val gulps = (2.0 * lodFactor).roundToInt().coerceAtLeast(1)
            for (g in 0 until gulps) {
                val rr = rnd.nextDouble() * rx * 0.45
                val theta = rnd.nextDouble() * 2.0 * PI
                val px = cos(theta) * rr
                val py = sin(theta) * rr
                val pos = Location(
                    world,
                    location.x + right.x * px + upPlane.x * py + normal.x * (-0.6 * depthBack),
                    location.y + right.y * px + upPlane.y * py + normal.y * (-0.6 * depthBack),
                    location.z + right.z * px + upPlane.z * py + normal.z * (-0.6 * depthBack)
                )
                dustBase.clone()
                    .data(Particle.DustOptions(darkCenter, 1.6f))
                    .location(pos)
                    .receivers(viewersRadius, true)
                    .spawn()
            }
        }
    }

    override fun boundingBox(height: Double): BoundingBox {
        val cx = location.x
        val cz = location.z
        val cy = location.y
        val r = radius
        val minX = cx - r
        val maxX = cx + r
        val minZ = cz - r
        val maxZ = cz + r
        val minY = cy - (height + 2 / 2.0)
        val maxY = cy + (height + 2 / 2.0)
        return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
    }

}