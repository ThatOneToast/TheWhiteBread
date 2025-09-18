package dev.theWhiteBread.menus

import dev.theWhiteBread.miniMessage
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

typealias ClickAction = (InventoryClickEvent) -> Unit

data class MenuItem(val stack: ItemStack, var onClick: ClickAction)

fun createMenuReadyItem(
    displayName: String,
    lore: List<String>,
    material: Material,
    enchantGlint: Boolean = false
): ItemStack {
    val item = ItemStack(material)
    val meta = item.itemMeta
    meta.displayName(miniMessage.deserialize(displayName).decorations(setOf(TextDecoration.ITALIC), false))
    meta.lore(lore.map { miniMessage.deserialize(it).decorations(setOf(TextDecoration.ITALIC), false) })
    meta.setEnchantmentGlintOverride(enchantGlint)
    item.itemMeta = meta
    return item
}

fun createMenuReadyItemFromItemStack(
    item: ItemStack,
    enchantGlint: Boolean = false,
    lore: List<String> = emptyList(),
    displayName: String? = null
): ItemStack {
    val meta = item.itemMeta
    if (displayName != null) meta.displayName(miniMessage.deserialize(displayName))
    meta.lore(lore.map { miniMessage.deserialize(it).decorations(setOf(TextDecoration.ITALIC), false) })
    meta.setEnchantmentGlintOverride(enchantGlint)
    item.itemMeta = meta
    return item
}

enum class Filter(
    val displayName: String,
    val icon: Material,
    val predicate: (ItemStack) -> Boolean
) {
    ALL("All", Material.PAPER, { true }),

    VALUABLES("Valuables", Material.NETHER_STAR, { item ->
        ItemCategories.isValuable(item.type)
    }),

    HEADS("Heads", Material.PLAYER_HEAD, { item ->
        ItemCategories.isHead(item.type)
    }),

    TOOLS("Tools", Material.IRON_PICKAXE, { item ->
        ItemCategories.isTool(item.type)
    }),

    BLOCKS("Blocks", Material.STONE, { item ->
        ItemCategories.isBlock(item.type)
    }),

    LIQUIDS("Liquids", Material.WATER_BUCKET, { item ->
        ItemCategories.isLiquid(item.type)
    }),

    SPAWN_EGGS("Spawn Eggs", Material.EGG, { item ->
        ItemCategories.isSpawnEgg(item.type)
    }),

    REDSTONE("Redstone", Material.REDSTONE, { item ->
        ItemCategories.isRedstone(item.type)
    }),

    ARMOR("Armor", Material.CHAINMAIL_CHESTPLATE, { item ->
        ItemCategories.isArmor(item.type)
    });
}

object ItemCategories {
    private val NON_BLOCK_SUFFIXES = setOf(
        "_PICKAXE", "_SHOVEL", "_AXE", "_HOE", "_SWORD",
        "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS",
        "_SPAWN_EGG", "_BUCKET", "_BOTTLE", "_POTION", "_BOAT"
    )

    private val TOOL_EXTRAS = setOf(
        Material.SHEARS, Material.FISHING_ROD,
        Material.FLINT_AND_STEEL, Material.BOW, Material.CROSSBOW,
        Material.TRIDENT
    )

    private val VALUABLE_EXACT = setOf(
        Material.DIAMOND, Material.DIAMOND_BLOCK, Material.DIAMOND_ORE,
        Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP,
        Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS,
        Material.NETHER_STAR, Material.BEACON, Material.TOTEM_OF_UNDYING,
        Material.ELYTRA, Material.DRAGON_EGG, Material.ENDER_PEARL,
        Material.ENDER_EYE,
        Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLD_ORE,
        Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_ORE,
        Material.EMERALD, Material.EMERALD_BLOCK, Material.EMERALD_ORE,
        Material.LAPIS_LAZULI, Material.LAPIS_BLOCK, Material.LAPIS_ORE,
        // raw forms (1.17+) — helpful to include if your server supports them
        Material.RAW_GOLD, Material.RAW_IRON
    )

    private val PRECIOUS_PREFIXES = listOf(
        "DIAMOND", "GOLD", "IRON", "EMERALD", "NETHERITE", "LAPIS"
    )

    fun isTool(material: Material): Boolean {
        val n = material.name
        return n.endsWith("_PICKAXE") ||
                n.endsWith("_SHOVEL") ||
                n.endsWith("_AXE") ||
                n.endsWith("_HOE") ||
                n.endsWith("_SWORD") ||
                TOOL_EXTRAS.contains(material)
    }

    fun isArmor(material: Material): Boolean {
        val n = material.name
        return n.endsWith("_HELMET") ||
                n.endsWith("_CHESTPLATE") ||
                n.endsWith("_LEGGINGS") ||
                n.endsWith("_BOOTS") ||
                material == Material.SHIELD ||
                material == Material.ELYTRA
    }

    fun isBlock(material: Material): Boolean {
        val n = material.name
        return NON_BLOCK_SUFFIXES.none { n.endsWith(it) } &&
                !n.contains("REDSTONE") &&
                !n.contains("REPEATER") &&
                !n.contains("COMPARATOR") &&
                !n.contains("HOPPER") &&
                !n.contains("LAVA") &&
                !n.contains("WATER")
    }

    fun isLiquid(material: Material): Boolean {
        val n = material.name
        return n.contains("WATER") ||
                n.contains("LAVA") ||
                material == Material.WATER_BUCKET ||
                material == Material.LAVA_BUCKET
    }

    fun isSpawnEgg(material: Material): Boolean {
        return material.name.endsWith("_SPAWN_EGG")
    }

    fun isRedstone(material: Material): Boolean {
        val n = material.name
        return n.contains("REDSTONE") ||
                n.contains("REPEATER") ||
                n.contains("COMPARATOR") ||
                n.contains("HOPPER") ||
                n.contains("DAYLIGHT_DETECTOR") ||
                n.contains("PISTON") ||
                n.contains("LEVER") ||
                n.contains("BUTTON")
    }

    fun isHead(material: Material): Boolean {
        val n = material.name
        return n.endsWith("_HEAD") || n.endsWith("_SKULL") ||
                material == Material.PLAYER_HEAD
    }

    fun isValuable(material: Material): Boolean {
        val n = material.name

        // explicit known valuables
        if (VALUABLE_EXACT.contains(material)) return true

        // match precious ores/blocks/ingots/nuggets by prefix
        if (PRECIOUS_PREFIXES.any { prefix ->
                n.startsWith(prefix) &&
                        (n.endsWith("_ORE") || n.endsWith("_BLOCK") ||
                                n.endsWith("_INGOT") || n.endsWith("_NUGGET"))
            }) return true

        // some fancy items not covered above
        if (n == "DRAGON_EGG" || n == "ENDER_PEARL" || n == "EYE_OF_ENDER") {
            return true
        }

        return false
    }
}