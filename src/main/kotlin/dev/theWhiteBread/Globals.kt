package dev.theWhiteBread

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.portals.DimensionalReceiver
import dev.theWhiteBread.portals.portal.Portal
import dev.theWhiteBread.serializables.InventoryData
import dev.theWhiteBread.serializables.SerializableLocation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.data.type.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


const val AREWEDEBUGGING: Boolean = true

object Keys {
    val getPlayerInput = NamespacedKey(TheWhiteBread.instance, "get_input_from_user")
    val veinMinerEnchantment = Key.key("whitebread:veinminer")
    val veinMineStyle = NamespacedKey(TheWhiteBread.instance, "vein_style")

    val storageControllers = NamespacedKey(TheWhiteBread.instance, "storage_controllers")
    val storageController = NamespacedKey(TheWhiteBread.instance, "storage_controller")
    val storageControllerRecipe = NamespacedKey(TheWhiteBread.instance, "storage_controller_recipe")
    val storageContainer = create("storage_container")

    val dimensialReceiver = NamespacedKey(TheWhiteBread.instance, "dimensial_receiver")

    val portals = NamespacedKey(TheWhiteBread.instance, "portal")

    fun create(key: String): NamespacedKey = NamespacedKey(TheWhiteBread.instance, key)

}


@Suppress("UNUSED")
object PDC {
    val playerStorageControllerList = pdcDataTypeOf<List<SerializableLocation>>()
    val playerManagerOfStorageControllers = pdcDataTypeOf<List<SerializableLocation>>()

    val portals = pdcDataTypeOf<Map<String, Portal>>()
    val dimensialReceiver = pdcDataTypeOf<DimensionalReceiver>()


    val defaultJson = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }



    inline fun <reified V: Any> valueOf(
        pdc: PersistentDataContainer,
        key: String
    ): V? = pdc.get(Keys.create(key), pdcDataTypeOf<V>())

    inline fun <reified V: Any> valueOf(
        pdc: PersistentDataContainer,
        key: NamespacedKey
    ): V? = pdc.get(key, pdcDataTypeOf<V>())

    inline fun <reified V: Any> getOrCompute(
        pdc: PersistentDataContainer,
        key: String,
        default: V
    ): V {
        val value = pdc.get(Keys.create(key), pdcDataTypeOf<V>())
        if (value == null) {
            pdc.set(Keys.create(key), pdcDataTypeOf<V>(), default)
            return default
        }
        return value
    }

    inline fun <reified V: Any> setValueOf(
        pdc: PersistentDataContainer,
        key: String,
        data: V
    ): V {
        pdc.set(Keys.create(key), pdcDataTypeOf<V>(), data)
        return data
    }

    inline fun <reified V: Any> setValueOf(
        pdc: PersistentDataContainer,
        key: NamespacedKey,
        data: V
    ): V {
        pdc.set(key, pdcDataTypeOf<V>(), data)
        return data
    }

    fun hasValueOf(
        pdc: PersistentDataContainer,
        key: NamespacedKey
    ): Boolean {
        return pdc.has(key)
    }

    fun hasValueOf(
        pdc: PersistentDataContainer,
        key: String
    ): Boolean {
        return pdc.has(Keys.create(key))
    }

    fun deleteData(pdc: PersistentDataContainer, vararg keys: String) {
        keys.forEach { pdc.remove(Keys.create(it)) }
    }


    inline fun <reified T : Any> pdcDataTypeOf(json: Json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }): PersistentDataType<ByteArray, T> {
        val ser: KSerializer<T> = serializer()

        return object : PersistentDataType<ByteArray, T> {
            override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java
            override fun getComplexType(): Class<T> = T::class.java

            override fun toPrimitive(
                complex: T,
                context: PersistentDataAdapterContext
            ): ByteArray {
                val s = json.encodeToString(ser, complex)
                return s.toByteArray(Charsets.UTF_8)
            }

            override fun fromPrimitive(
                primitive: ByteArray,
                context: PersistentDataAdapterContext
            ): T {
                val s = primitive.toString(Charsets.UTF_8)
                return json.decodeFromString(ser, s)
            }
        }
    }
}

val miniMessage = MiniMessage
    .builder()
    .tags(TagResolver.resolver(TagResolver.builder()
    .tag("i", Tag.styling(TextDecoration.ITALIC.withState(false)))
    .tag("italic", Tag.styling(TextDecoration.ITALIC.withState(false)))
    .build(),TagResolver.standard()))
    .build()

fun createHikariDataSource(jdbcUrl: String, user: String, pass: String): HikariDataSource {
    val cfg = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.username = user
        this.password = pass
        maximumPoolSize = 10
        minimumIdle = 2
        isAutoCommit = false
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    }
    return HikariDataSource(cfg)
}

val createPlayerHead: (OfflinePlayer) -> ItemStack = { player -> run {
    val head = ItemStack(Material.PLAYER_HEAD)
    val meta = head.itemMeta as SkullMeta
    meta.owningPlayer = Bukkit.getOfflinePlayer(player.uniqueId)
    meta.displayName(miniMessage.deserialize("<gold>${player.name}"))
    head.itemMeta = meta
    head
}}

fun String.toUUID(): UUID {
    return UUID.fromString(this)
}

fun Location.toSerLocation(): SerializableLocation {
    return SerializableLocation(
        this.world.uid.toString(),
        this.x, this.y, this.z,
        packInts(this.chunk.x, this.chunk.z)
    )
}

fun MutableList<Block>.toSerializedLocationList(): List<SerializableLocation> {
    val locationList = mutableListOf<SerializableLocation>()
    this.forEach { locationList.add(SerializableLocation.toSerialized(it.location)) }
    return locationList
}

fun MutableMap<Chest.Type, MutableList<Block>>.toSerializedLocationMap(): MutableMap<Chest.Type, MutableList<SerializableLocation>> {
    val serializedMap = mutableMapOf<Chest.Type, MutableList<SerializableLocation>>()

    this.forEach {
        serializedMap[it.key] = it.value.toSerializedLocationList().toMutableList()
    }

    return serializedMap
}

fun MutableCollection<SerializableLocation>.toBlocks(): MutableCollection<Block> {
    return this.map { it.toBlock() }.toMutableSet()
}

fun parsePipedStrings(input: String): List<String> =
    input.split('|').map { it.trim() }.filter { it.isNotEmpty() }

fun locationToString(loc: Location, useDoubles: Boolean = false): String {
    fun trimNum(d: Double): String =
        if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

    val worldName = loc.world?.name ?: "unknown"
    val x = if (useDoubles) loc.x else loc.blockX.toDouble()
    val y = if (useDoubles) loc.y else loc.blockY.toDouble()
    val z = if (useDoubles) loc.z else loc.blockZ.toDouble()
    return "world:$worldName,x:${trimNum(x)},y:${trimNum(y)},z:${trimNum(z)}"
}

fun parseLocation(input: String): Location? {
    val map = input
        .split(',')
        .mapNotNull { part ->
            val idx = part.indexOf(':')
            if (idx < 0) return@mapNotNull null
            val key = part.take(idx).trim().lowercase()
            val value = part.substring(idx + 1).trim()
            key to value
        }.toMap()

    val x = map["x"]?.toDoubleOrNull() ?: return null
    val y = map["y"]?.toDoubleOrNull() ?: return null
    val z = map["z"]?.toDoubleOrNull() ?: return null

    val worldName = map["world"] ?: map["dimension"] ?: map["worldname"]
    var world: World? = worldName?.let { Bukkit.getWorld(it) }

    if (world == null) {
        val uuidStr = map["worlduuid"] ?: map["uuid"]
        if (!uuidStr.isNullOrBlank()) {
            runCatching { UUID.fromString(uuidStr) }
                .getOrNull()
                ?.let { world = Bukkit.getWorld(it) }
        }
    }

    world ?: return null
    return Location(world, x, y, z)
}



/**
 * Convert an RGB integer to a Bukkit Color.
 *
 * The input integer must be in 0xRRGGBB format (red in the
 * highest byte, green next, blue lowest). Any alpha byte (if
 * present) is ignored.
 *
 * Example:
 *  intToColor(0xFF0000) -> Color.fromRGB(255, 0, 0) // red
 *
 * @param rgb Int containing RGB in 0xRRGGBB format
 * @return Color with red/green/blue components in 0..255
 */
fun intToColor(rgb: Int): Color {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return Color.fromRGB(r, g, b)
}

/**
 * Multiply each RGB channel of a Color by [factor].
 *
 * Useful to produce darker (factor < 1.0) or brighter
 * (factor > 1.0) variants. Each channel is rounded to the
 * nearest integer and clamped to 0..255 to avoid overflow.
 *
 * Note: this operates only on RGB; Bukkit Color has no alpha here.
 *
 * @param c input Color
 * @param factor multiplier applied to each channel
 * @return new Color with scaled channels (clamped to 0..255)
 */
fun colorMul(c: Color, factor: Double): Color =
    Color.fromRGB(
        (c.red * factor).roundToInt().coerceIn(0, 255),
        (c.green * factor).roundToInt().coerceIn(0, 255),
        (c.blue * factor).roundToInt().coerceIn(0, 255)
    )

/**
 * Linearly interpolate (lerp) between two Colors [a] and [b].
 *
 * When [tMix] == 0.0 returns [a], when [tMix] == 1.0 returns [b].
 * Values outside 0..1 are accepted (result is extrapolated then
 * clamped to 0..255). Components are interpolated independently
 * and rounded to nearest integer.
 *
 * @param a start Color
 * @param b end Color
 * @param tMix interpolation factor (0.0..1.0 typical)
 * @return interpolated Color (components clamped to 0..255)
 */
fun lerpColor(a: Color, b: Color, tMix: Double): Color {
    fun mixInt(x: Int, y: Int, t2: Double) =
        (x * (1.0 - t2) + y * t2).roundToInt().coerceIn(0, 255)

    return Color.fromRGB(
        mixInt(a.red, b.red, tMix),
        mixInt(a.green, b.green, tMix),
        mixInt(a.blue, b.blue, tMix)
    )
}

fun Vec3.toLocation(worldUID: UUID): Location {
    return Location(Bukkit.getWorld(worldUID)!!, this.x, this.y, this.z )
}

fun Vec3.roundTo2Place(mode: RoundingMode = RoundingMode.HALF_UP): Vec3 {
    fun r(d: Double) = BigDecimal.valueOf(d).setScale(2, mode).toDouble()
    return Vec3(r(this.x), r(this.y), r(this.z))
}

fun String.toComponent(): Component {
    return miniMessage.deserialize(this)
}


fun outlineBlock(
    block: Block,
    viewer: Player? = null, // show to a specific player if non-null
    stepsPerEdge: Int = 3, // particle count per edge (lower = fewer particles)
    thicknessSteps: Int = 0, // how many offsets outwards (0 = single thin line)
    thicknessSpacing: Double = 0.12, // spacing between offsets
    particleSize: Float = 0.65f, // Dust particle size (visual thickness)
    howLongSeconds: Long? = null // optional duration in seconds (null = draw once)
) {
    val w = block.world
    val bx = block.x
    val by = block.y
    val bz = block.z

    // only draw if at least one face is exposed to air
    val neighbours = arrayOf(
        w.getBlockAt(bx - 1, by, bz),
        w.getBlockAt(bx + 1, by, bz),
        w.getBlockAt(bx, by - 1, bz),
        w.getBlockAt(bx, by + 1, bz),
        w.getBlockAt(bx, by, bz - 1),
        w.getBlockAt(bx, by, bz + 1)
    )
    if (neighbours.none { it.type.isAir }) return

    val origin = block.location.toVector()
    val ox = origin.x
    val oy = origin.y
    val oz = origin.z

    val corners = listOf(
        Vector(ox, oy, oz),
        Vector(ox + 1.0, oy, oz),
        Vector(ox, oy + 1.0, oz),
        Vector(ox + 1.0, oy + 1.0, oz),
        Vector(ox, oy, oz + 1.0),
        Vector(ox + 1.0, oy, oz + 1.0),
        Vector(ox, oy + 1.0, oz + 1.0),
        Vector(ox + 1.0, oy + 1.0, oz + 1.0)
    )

    val edges = listOf(
        Pair(corners[0], corners[1]),
        Pair(corners[2], corners[3]),
        Pair(corners[4], corners[5]),
        Pair(corners[6], corners[7]),
        Pair(corners[0], corners[2]),
        Pair(corners[1], corners[3]),
        Pair(corners[4], corners[6]),
        Pair(corners[5], corners[7]),
        Pair(corners[0], corners[4]),
        Pair(corners[1], corners[5]),
        Pair(corners[2], corners[6]),
        Pair(corners[3], corners[7])
    )

    // Colored dust uses Particle.REDSTONE
    val dust = Particle.DustOptions(Color.fromRGB(0, 255, 0), particleSize)

    // collect unique positions (avoid duplicates)
    val positions = LinkedHashSet<Location>()
    fun addPos(pos: Vector) {
        fun round(d: Double) = (d * 1000.0).roundToInt() / 1000.0
        val x = round(pos.x)
        val y = round(pos.y)
        val z = round(pos.z)
        positions.add(Location(w, x, y, z))
    }

    val steps = max(1, stepsPerEdge)
    val thick = max(0, thicknessSteps)

    for ((a, b) in edges) {
        val dir = b.clone().subtract(a)
        val axisX = abs(dir.x) > 0.5
        val axisY = abs(dir.y) > 0.5
        val axisZ = abs(dir.z) > 0.5

        val perp1 = when {
            axisX -> Vector(0.0, 1.0, 0.0) // Y
            axisY -> Vector(1.0, 0.0, 0.0) // X
            else -> Vector(1.0, 0.0, 0.0) // X (edge is along Z)
        }
        val perp2 = when {
            axisX -> Vector(0.0, 0.0, 1.0) // Z
            axisY -> Vector(0.0, 0.0, 1.0) // Z
            else -> Vector(0.0, 1.0, 0.0) // Y (edge is along Z)
        }

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val point = a.clone().add(dir.clone().multiply(t))
            for (sx in -thick..thick) {
                for (sy in -thick..thick) {
                    val offset = perp1.clone().multiply(sx * thicknessSpacing)
                        .add(perp2.clone().multiply(sy * thicknessSpacing))
                    val pos = point.clone().add(offset)
                    addPos(pos)
                }
            }
        }
    }

    // draw once
    for (loc in positions) {
        if (viewer != null) {
            viewer.spawnParticle(
                Particle.DUST,
                loc,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                dust
            )
        } else {
            w.spawnParticle(
                Particle.DUST,
                loc,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                dust
            )
        }
    }

    // optionally repeat for duration (every 4 ticks)
    if (howLongSeconds != null && howLongSeconds > 0) {
        object : BukkitRunnable() {
            var ticksLeft = (howLongSeconds * 20).toInt()
            override fun run() {
                if (ticksLeft <= 0) {
                    cancel()
                    return
                }
                outlineBlock(
                    block,
                    viewer,
                    stepsPerEdge,
                    thicknessSteps,
                    thicknessSpacing,
                    particleSize,
                    howLongSeconds = null
                )
                ticksLeft -= 4
            }
        }.runTaskTimer(TheWhiteBread.instance, 0L, 4L)
    }
}

private fun horizontalStepFromYaw(
    yaw: Float,
    diagonals: Boolean = true
): Pair<Int, Int> {
    val y = ((yaw % 360) + 360) % 360 // normalize to [0, 360)
    return if (diagonals) {
        when {
            y !in 22.5..<337.5 -> 0 to 1       // SOUTH
            y < 67.5 -> -1 to 1                    // SOUTH_WEST
            y < 112.5 -> -1 to 0                   // WEST
            y < 157.5 -> -1 to -1                  // NORTH_WEST
            y < 202.5 -> 0 to -1                   // NORTH
            y < 247.5 -> 1 to -1                   // NORTH_EAST
            y < 292.5 -> 1 to 0                    // EAST
            else -> 1 to 1                         // SOUTH_EAST
        }
    } else {
        when {
            y !in 45.0..<315.0 -> 0 to 1           // SOUTH
            y < 135 -> -1 to 0                     // WEST
            y < 225 -> 0 to -1                     // NORTH
            else -> 1 to 0                         // EAST
        }
    }
}

fun nextAirBlockOnSameY(
    player: Player,
    origin: Location,
    maxBlocks: Int = 64,
    allowDiagonals: Boolean = true
): Block? {
    val (dx, dz) = horizontalStepFromYaw(player.location.yaw, allowDiagonals)
    val world = origin.world
    var x = origin.blockX
    val y = origin.blockY
    var z = origin.blockZ

    repeat(maxBlocks) {
        x += dx
        z += dz
        val b = world.getBlockAt(x, y, z)
        if (b.type.isAir) return b
    }
    return null
}
/**
 * If you only want the immediately adjacent block (same Y) in the look
 * direction, regardless of AIR or not, use this:
 */
fun adjacentBlockSameY(
    player: Player,
    origin: Location,
    allowDiagonals: Boolean = true,
    returnOnAir: Boolean = false
): Block? {
    val (dx, dz) = horizontalStepFromYaw(player.location.yaw, allowDiagonals)
    val block = origin.block.getRelative(dx, 0, dz)
    if (block.type == Material.AIR) return null
    return block
}

fun ItemStack.getId(key: NamespacedKey): UUID? {
    val meta = this.itemMeta ?: return null
    val str = meta.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return null
    return try {
        UUID.fromString(str)
    } catch (ex: IllegalArgumentException) {
        null
    }
}

fun ItemStack.setRandomId(key: NamespacedKey) {
    val meta = this.itemMeta ?: return
    meta.persistentDataContainer.set(key, PersistentDataType.STRING, UUID.randomUUID().toString())
    this.itemMeta = meta
}

fun ItemStack.getOrCreateId(key: NamespacedKey): UUID? {
    val meta = this.itemMeta ?: return null
    val pdc = meta.persistentDataContainer

    val existing = pdc.get(key, PersistentDataType.STRING)
    if (existing != null) return UUID.fromString(existing)

    val id = UUID.randomUUID()
    pdc.set(key, PersistentDataType.STRING, id.toString())
    this.itemMeta = meta
    return id
}

inline fun <reified D : Any> ItemStack.setData(name: String, data: D) {
    val ns = Keys.create("data-$name")
    val meta = this.itemMeta!!

    val json = PDC.defaultJson.encodeToString(serializer<D>(), data)
    meta.persistentDataContainer.set(ns, PersistentDataType.BYTE_ARRAY, json.toByteArray(Charsets.UTF_8))

    this.itemMeta = meta
}

inline fun <reified D : Any> ItemStack.getData(name: String): D? {
    val ns = Keys.create("data-$name")

    val meta = this.itemMeta ?: return null
    val bytes = meta.persistentDataContainer.get(ns, PersistentDataType.BYTE_ARRAY) ?: return null
    val json = bytes.toString(Charsets.UTF_8)

    val value = PDC.defaultJson.decodeFromString(serializer<D>(), json)
    return value
}

inline fun <reified D : Any> ItemStack.computeData(name: String, default: D): D {
    val got = getData<D>(name)
    if (got != null) return got
    setData(name, default)
    return default
}

fun<C> Collection<C>.mapToString(): Collection<String> = this.map { it.toString() }

fun Inventory.removeAmount(material: Material, amount: Int = 1): Boolean {
    if (amount <= 0) return true

    var total = 0
    for (slot in 0 until size) {
        val stack = getItem(slot) ?: continue
        if (stack.type != material) continue
        total += stack.amount
        if (total >= amount) break
    }
    if (total < amount) return false

    var toRemove = amount
    for (slot in 0 until size) {
        if (toRemove == 0) break
        val stack = getItem(slot) ?: continue
        if (stack.type != material) continue

        val take = minOf(stack.amount, toRemove)
        val newAmount = stack.amount - take
        toRemove -= take

        if (newAmount <= 0) {
            clear(slot)
        } else {
            stack.amount = newAmount
            setItem(slot, stack)
        }
    }
    return true
}

fun packInts(a: Int, b: Int): Long {
    return ((a.toLong() and 0xFFFFFFFFL) shl 32) or (b.toLong() and 0xFFFFFFFFL)
}

fun unpackInts(value: Long): Pair<Int, Int> {
    val a = (value shr 32).toInt()
    val b = value.toInt()
    return Pair(a, b)
}

fun Location.chunkKey(): Long {
    return packInts(this.chunk.x, this.chunk.z)
}

/**
 * Draw 4 vertical green lines at the chunk corners for one instant draw (single tick).
 *
 * @param player target player to show the particles to (use player.spawnParticle for minimal traffic)
 * @param chunk chunk to outline
 * @param halfHeight how many blocks above and below the player's Y to draw (default 6 => ~13 blocks tall)
 * @param spacing vertical spacing between particles in blocks (bigger = fewer particles)
 * @param particleSize dust particle size (0.5..1.5 typical)
 */
fun showChunkCornerLines(
    player: Player,
    chunk: Chunk,
    halfHeight: Int = 6,
    spacing: Int = 2,
    particleSize: Float = 1.0f
) {
    val world = player.world
    if (world != chunk.world) return // avoid cross-world

    val playerY = player.location.y
    val startY = (playerY - halfHeight).coerceAtLeast(0.0).toInt()
    val endY = (playerY + halfHeight).toInt()

    val baseX = (chunk.x shl 4).toDouble() // chunkX * 16
    val baseZ = (chunk.z shl 4).toDouble() // chunkZ * 16

    // Center the particle inside the corner block
    val xs = listOf(baseX + 0.5, baseX + 15.5)
    val zs = listOf(baseZ + 0.5, baseZ + 15.5)

    val dust = Particle.DustOptions(Color.fromRGB(0, 255, 0), particleSize)

    for (x in xs) {
        for (z in zs) {
            var y = startY
            while (y <= endY) {
                val loc = Location(world, x, y.toDouble(), z)
                // spawn a single dust particle at this exact location for just this player
                player.spawnParticle(Particle.DUST, loc, 1, dust)
                y += spacing
            }
        }
    }
}

/**
 * Repeatedly show the chunk corner lines to a player for a duration.
 *
 * @param plugin your plugin instance for scheduling
 * @param player player to show to
 * @param chunk chunk to highlight
 * @param durationTicks how long to show (in server ticks). Default 200 ticks (10s).
 * @param intervalTicks how often to re-draw (in ticks). Default 10 ticks (0.5s).
 * Other parameters forwarded to showChunkCornerLines.
 */
fun highlightChunkFor(
    plugin: JavaPlugin,
    player: Player,
    chunk: Chunk,
    durationTicks: Long = 100,
    intervalTicks: Long = 10,
    halfHeight: Int = 6,
    spacing: Int = 2,
    particleSize: Float = 1.0f
) {
    val task = object : BukkitRunnable() {
        var elapsed = 0L
        override fun run() {
            if (!player.isOnline || player.world != chunk.world || elapsed >= durationTicks) {
                cancel()
                return
            }
            showChunkCornerLines(player, chunk, halfHeight, spacing, particleSize)
            elapsed += intervalTicks
        }
    }
    task.runTaskTimer(plugin, 0L, intervalTicks)
}



fun Inventory.toData(): InventoryData {
    val contentsIterator = this.iterator().withIndex()

    val baMap = mutableMapOf<Int, ByteArray>()

    while (contentsIterator.hasNext()) {
        val item = contentsIterator.next()
        if (item.value == null) continue
        baMap[item.index] = item.value.serializeAsBytes()
    }

    return InventoryData(
        baMap.toMap(),
        this.size
    )
}