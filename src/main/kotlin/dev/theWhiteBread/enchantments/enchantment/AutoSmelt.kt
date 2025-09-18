package dev.theWhiteBread.enchantments.enchantment

import dev.theWhiteBread.enchantments.Enchantment
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ExperienceOrb
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

@Suppress("UnstableApiUsage")
object AutoSmelt {
    val enchantment = Enchantment(
        "whitebread:autosmelt",
        Component.text("Auto Smelt"),
        anvilCost = 5,
        minimumCost = EnchantmentRegistryEntry.EnchantmentCost.of(2, 2),
        maxiumCost = EnchantmentRegistryEntry.EnchantmentCost.of(5, 3),
        maxLevel = 1,
        activeSlots = EquipmentSlotGroup.MAINHAND,
        weight = 4,
        inEnchantmentTable = true
    )

    fun smeltedOf(mat: Material): Material? = when (mat) {
        // raw -> ingot
        Material.RAW_IRON -> Material.IRON_INGOT
        Material.RAW_GOLD -> Material.GOLD_INGOT
        Material.RAW_COPPER -> Material.COPPER_INGOT

        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> Material.IRON_INGOT
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT
        Material.ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP
        Material.NETHER_QUARTZ_ORE -> Material.QUARTZ

        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE -> Material.EMERALD
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE -> Material.COAL

        else -> null
    }

    /**
     * Return the smelted drops for [block] when broken with [tool].
     * - Uses block.getDrops(tool) so fortune/silk-touch/tool-type behavior is preserved.
     * - If the tool has Silk Touch, the raw drops are returned unchanged.
     * - Maps common ores/raw items to their "smelted" equivalents (ingots, scrap, etc.).
     */
    fun getAutoSmeltedDrops(block: Block, tool: ItemStack): List<ItemStack> {
        val rawDrops = block.getDrops(tool).map { it.clone() }

        // Preserve Silk Touch
        if (tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) return rawDrops

        return rawDrops.map { drop ->
            val target = smeltedOf(drop.type)
            if (target == null) drop else ItemStack(target, drop.amount)
        }
    }

    val spawnExperienceOrb: (Location, Int) -> ExperienceOrb? = { loc, amount ->
        if (amount <= 0) null
        else {
            val orb = loc.world!!.spawn(loc, ExperienceOrb::class.java)
            orb.experience = amount
            orb
        }
    }

    fun vanillaXpForBlock(block: Block, tool: ItemStack?): Int {
        if (tool == null) return 0
        if (tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) return 0

        return when (block.type) {
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE -> Random.nextInt(1, 3)
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE -> Random.nextInt(3, 8)
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE -> Random.nextInt(3, 8)
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE -> Random.nextInt(2, 6)
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE -> Random.nextInt(1, 6)
            Material.NETHER_QUARTZ_ORE -> Random.nextInt(2, 6)
            Material.ANCIENT_DEBRIS -> Random.nextInt(2, 6)
            else -> 0
        }
    }
}