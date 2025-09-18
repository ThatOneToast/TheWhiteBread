package dev.theWhiteBread

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.portals.DimensionalReceiver
import dev.theWhiteBread.portals.Portal
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.container.StorageContainer
import dev.theWhiteBread.storage_system.container.StorageContainerVersions
import dev.theWhiteBread.storage_system.container.getBlock
import dev.theWhiteBread.storage_system.controller.StorageControllerData
import dev.theWhiteBread.storage_system.controller.StorageControllerDataVersions
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.NamespacedKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.collections.forEach
import kotlin.math.roundToInt


const val AREWEDEBUGGING: Boolean = true

object Keys {
    val getPlayerInput = NamespacedKey(TheWhiteBread.instance, "get_input_from_user")
    val veinMinerEnchantment = Key.key("whitebread:veinminer")
    val veinMineStyle = NamespacedKey(TheWhiteBread.instance, "vein_style")

    val itemStorageController = NamespacedKey(TheWhiteBread.instance, "storage_controller_1x")
    val itemStorageControllerManagerOf = NamespacedKey(TheWhiteBread.instance, "storage_controller_manager_of")
    val itemStorageContainer = NamespacedKey(TheWhiteBread.instance, "storage_controller_container")
    val playerStorageControllers = NamespacedKey(TheWhiteBread.instance, "player_storage_controllers")
    val itemStorageControllerRecipe = NamespacedKey(TheWhiteBread.instance, "storage_controller_1x_recipe")

    val dimensialReceiver = NamespacedKey(TheWhiteBread.instance, "dimensial_receiver")

    val itemAbility = NamespacedKey(TheWhiteBread.instance, "item_ability")
    val portals = NamespacedKey(TheWhiteBread.instance, "portal")
}


@Suppress("UNUSED")
object PDC {
    val playerStorageControllerList = pdcDataTypeOf<List<SerializableLocation>>()
    val playerManagerOfStorageControllers = pdcDataTypeOf<List<SerializableLocation>>()

    val chunkStorageControllerDataVersion1 = pdcDataTypeOf<StorageControllerDataVersions.Version1>()
    val chunkStorageControllerDataVersion2 = pdcDataTypeOf<StorageControllerDataVersions.Version2>()
    val chunkStorageControllerDataVersion3 = pdcDataTypeOf<StorageControllerDataVersions.Version3>()

    val storageContainerVersion1 = pdcDataTypeOf<StorageContainerVersions.Version1>()
    val storageContainerVersion2 = pdcDataTypeOf<StorageContainerVersions.Version2>()

    val portals = pdcDataTypeOf<Map<String, Portal>>()
    val dimensialReceiver = pdcDataTypeOf<DimensionalReceiver>()


    /**
     * This is the latest version of ChunkStorageContainer
     *
     * **Important for migration notices**
     */
    val chunkStorageContainer = pdcDataTypeOf<StorageContainer>()

    /**
     * This is the latest version of StorageController
     *
     * **Important for migration notices**
     */
    val storageControllerData = pdcDataTypeOf<StorageControllerData>()

    inline fun <reified T : Any> pdcDataTypeOf(
        json: Json = Json { encodeDefaults = false; ignoreUnknownKeys = true }
    ): PersistentDataType<ByteArray, T> {
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
    return SerializableLocation(this.world.uid.toString(), this.x, this.y, this.z)
}

fun MutableList<Block>.toSerializedLocationList(): List<SerializableLocation> {
    val locationList = mutableListOf<SerializableLocation>()
    this.forEach { locationList.add(SerializableLocation.toSerialized(it.location)) }
    return locationList
}

fun MutableMap<org.bukkit.block.data.type.Chest.Type, MutableList<Block>>.toSerializedLocationMap(): MutableMap<org.bukkit.block.data.type.Chest.Type, MutableList<SerializableLocation>> {
    val serializedMap = mutableMapOf<org.bukkit.block.data.type.Chest.Type, MutableList<SerializableLocation>>()

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
 * Update the blocks PDC with new `ChunkStorageContainer` value
 * This function will update it's corresponding controller with new information.
 */
fun Container.updateStorageContainer(value: StorageContainer) {
    this.persistentDataContainer.set(Keys.itemStorageContainer, PDC.chunkStorageContainer, value)
    this.update(true, false)
    val controller = ControllerRegistry.getController(value.parentControllerLocation.fromSerLocation().chunk)
        ?: throw IllegalStateException("No parent controller to update container for.")
    controller.updateViewState()
    controller.save()
}

fun String.replaceVars(
    vars: Map<String, String>,
    onMissing: (String) -> String = { original -> $$"$$${original}" }
): String {
    val regex = Regex("""\$\{?([A-Za-z0-9_]+)}?""")
    return regex.replace(this) { match ->
        val key = match.groupValues[1]
        vars[key] ?: onMissing(key)
    }
}

fun List<StorageContainer>.toMenuItemList(action: (StorageContainer, InventoryClickEvent) -> Unit): List<MenuItem> {
    val list = mutableListOf<MenuItem>()

    this.forEach { container ->
        list.add(
            MenuItem(
                createMenuReadyItem(
                    container.customization.displayName ?: container.getBlock().type.toString(),
                    container.customization.description ?: listOf("Unknown"),
                    container.customization.displayItem ?: container.getBlock().type
                )
            ) { event -> action(container, event) })
    }

    return list
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