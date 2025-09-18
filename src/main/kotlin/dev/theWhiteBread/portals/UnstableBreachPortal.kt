package dev.theWhiteBread.portals

import dev.theWhiteBread.colorMul
import dev.theWhiteBread.intToColor
import dev.theWhiteBread.lerpColor
import dev.theWhiteBread.serializables.LocationSerializer
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
class UnstableBreachPortal(
    override val id: String,
    val owner: String?,
    @Serializable(with = LocationSerializer::class)
    override val location: Location,
    @Serializable(with = LocationSerializer::class)
    override val destination: Location? = null,
    override val portalType: PortalType = PortalType.UNSTABLE,
    val radius: Double = 1.5,
    override var persistence: Boolean = true,
    var color: Int = 0xFF0000
) : Portal {

    fun toBreachPortal() : BreachPortal? {
        owner ?: return null
        PortalManager.deRegisterPortal(id, true)
        return BreachPortal(id, owner, location, destination, radius, color = color).registerPortal(true)
    }


    override fun renderPortal() {
        val world = location.world ?: return
        val t = (System.currentTimeMillis() % 10_000L) / 1000.0

        // --- tunable's (adjust for look / perf) ---------------------------------
        val viewersRadius = 48                     // who receives the particles
        val depthLayers = 4                        // thickness layers
        val depthBlocks = 1.8                      // total thickness (blocks)
        val verticalScale = 0.9                    // oval vertical compression
        val rx = radius * 1.15                     // horizontal radius of ellipse
        val ry = rx * verticalScale                // vertical radius of ellipse
        val densityFactor = 6.0                    // particles per block-area
        val jitter = 0.06                          // small random offset
        val coneDepth = depthBlocks * 1.6         // how far the cone tip sits behind
        val coneLines = 10                         // cone lines (keeps cone connected)
        val coneSteps = 6                          // points per cone line
        // colors: dark center -> mid -> outer
        val base = intToColor(color)
        val darkCenter = colorMul(base, 0.08)      // very dark center
        val midColor = lerpColor(darkCenter, base, 0.45)
        val outerColor = lerpColor(base, Color.fromRGB(255, 255, 255), 0.25)
        // -----------------------------------------------------------------------

        val dustBase = Particle.DUST.builder().count(1)

        // choose facing normal: nearest viewer if nearby, else saved direction or +Z
        var normal = Vector(0.0, 0.0, 1.0)
        val nearest = Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.world == world }
            .minByOrNull { it.location.distanceSquared(location) }
        if (nearest != null &&
            nearest.location.distanceSquared(location) <= viewersRadius * viewersRadius
        ) {
            nearest.eyeLocation.toVector().subtract(location.toVector())
                .also { it.y = 0.0 }
                .takeIf { it.lengthSquared() > 1e-6 }
                ?.normalize()
                ?.let { normal = it }
        } else if (location.direction.lengthSquared() > 1e-6) {
            normal = location.direction.clone().also { it.y = 0.0 }.normalize()
        }

        var right = normal.clone().crossProduct(Vector(0.0, 1.0, 0.0))
        if (right.lengthSquared() < 1e-6) right = Vector(1.0, 0.0, 0.0)
        right.normalize()
        val upPlane = right.clone().crossProduct(normal).normalize()

        // compute particles per layer from ellipse area
        val area = Math.PI * rx * ry
        val perLayer = (area * densityFactor).roundToInt().coerceIn(30, 600)
        val rnd = Random.Default

        // --- filled oval: randomized sampling per layer (no concentric rings) ---
        for (layer in 0 until depthLayers) {
            val layerT = if (depthLayers == 1) 0.5 else layer.toDouble() / (depthLayers - 1)
            val depthOffset = -layerT * depthBlocks
            val depthVec = normal.clone().multiply(depthOffset)

            for (i in 0 until perLayer) {
                // uniform disk sampling mapped to ellipse
                val u = rnd.nextDouble()
                val rNorm = sqrt(u)                 // radial fraction 0..1
                val theta = rnd.nextDouble() * 2.0 * Math.PI
                var px = cos(theta) * rNorm * rx
                var py = sin(theta) * rNorm * ry
                // small random jitter to avoid patterning
                px += (rnd.nextDouble() - 0.5) * jitter * rx
                py += (rnd.nextDouble() - 0.5) * jitter * ry

                val spawn = Location(
                    world,
                    location.x + right.x * px + upPlane.x * py + depthVec.x,
                    location.y + right.y * px + upPlane.y * py + depthVec.y,
                    location.z + right.z * px + upPlane.z * py + depthVec.z
                )

                // color: depth makes it darker toward the inside, radial makes rim lighter
                val depthMix = lerpColor(darkCenter, midColor, layerT)
                val finalColor = lerpColor(depthMix, outerColor, rNorm)
                val size = ((1.05 - 0.6 * rNorm) * (1.0 - 0.22 * layerT)).toFloat()

                dustBase.clone()
                    .data(Particle.DustOptions(finalColor, size))
                    .location(spawn)                  // location BEFORE receivers()
                    .receivers(viewersRadius, true)
                    .spawn()
            }
        }

        // --- cone/backfill: connect the rim to a single tip so it reads flushly ---
        val tip = location.toVector().clone().add(normal.clone().multiply(-coneDepth))
        for (i in 0 until coneLines) {
            val ang = 2.0 * Math.PI * i / coneLines + t * 0.12
            val rimX = cos(ang) * rx
            val rimY = sin(ang) * ry
            val rimVec = location.toVector().clone()
                .add(right.clone().multiply(rimX))
                .add(upPlane.clone().multiply(rimY))

            for (step in 1..coneSteps) {
                val s = step.toDouble() / (coneSteps + 1.0)
                val px = rimVec.x * (1.0 - s) + tip.x * s
                val py = rimVec.y * (1.0 - s) + tip.y * s
                val pz = rimVec.z * (1.0 - s) + tip.z * s
                val spawn = Location(world, px, py, pz)

                val colorAlong = lerpColor(midColor, darkCenter, s * 0.9)
                val sizeAlong = (0.6 * (1.0 - 0.9 * s)).toFloat()

                dustBase.clone()
                    .data(Particle.DustOptions(colorAlong, sizeAlong))
                    .location(spawn)
                    .receivers(viewersRadius, true)
                    .spawn()
            }
        }

        // --- small dark core accent (optional, gives perceived depth) -----------
        val coreCount = 6
        for (k in 0 until coreCount) {
            val ct = k.toDouble() / (coreCount - 1).coerceAtLeast(1)
            val coreDepth = -ct * (depthBlocks * 0.9)
            val jitterAng = t * 1.9 + k
            val radial = rx * 0.12 * (1.0 - ct) * (0.7 + 0.4 * sin(jitterAng))
            val corePt = right.clone().multiply(cos(jitterAng) * radial)
                .add(upPlane.clone().multiply(sin(jitterAng) * radial))
                .add(normal.clone().multiply(coreDepth))
            val coreLoc = Location(
                world, location.x + corePt.x, location.y + corePt.y,
                location.z + corePt.z
            )

            dustBase.clone()
                .data(Particle.DustOptions(darkCenter, 1.2f - 0.2f * ct.toFloat()))
                .location(coreLoc)
                .receivers(viewersRadius, true)
                .spawn()
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