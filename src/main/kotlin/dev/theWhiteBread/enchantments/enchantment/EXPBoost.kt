package dev.theWhiteBread.enchantments.enchantment

import dev.theWhiteBread.enchantments.Enchantment
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import net.kyori.adventure.text.Component
import org.bukkit.entity.ExperienceOrb
import org.bukkit.inventory.EquipmentSlotGroup
import kotlin.math.ceil

@Suppress("UnstableApiUsage")
object EXPBoost {
    val enchantment = Enchantment(
        "whitebread:expboost",
        Component.text("EXP Boost"),
        supportedItems = ItemTypeTagKeys.LEG_ARMOR,
        anvilCost = 5,
        minimumCost = EnchantmentRegistryEntry.EnchantmentCost.of(2, 2),
        maxiumCost = EnchantmentRegistryEntry.EnchantmentCost.of(5, 3),
        maxLevel = 10,
        activeSlots = EquipmentSlotGroup.LEGS,
        weight = 4,
        inEnchantmentTable = true
    )

    fun applyExpBoost(baseXp: Int, boostLevel: Int): Int {
        if (baseXp <= 0 || boostLevel <= 0) return baseXp
        val multiplier = 1.0 + 0.10 * boostLevel
        return ceil(baseXp * multiplier).toInt()
    }

    /** Mutate an orb's stored XP (be careful: orb can be picked up by any player). */
    fun applyToOrb(orb: ExperienceOrb, boostLevel: Int) {
        orb.experience = applyExpBoost(orb.experience, boostLevel)
    }
}