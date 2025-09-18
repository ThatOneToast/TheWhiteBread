package dev.theWhiteBread.enchantments

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

@Suppress("UnstableApiUsage")
data class Enchantment(
    val key: String,
    val name: Component,
    val anvilCost: Int = 1,
    val maxLevel: Int = 2,
    val weight: Int = 10,
    val supportedItems: TagKey<ItemType> = ItemTypeTagKeys.ENCHANTABLE_MINING,
    val minimumCost: EnchantmentRegistryEntry.EnchantmentCost = EnchantmentRegistryEntry.EnchantmentCost.of(1, 1),
    val maxiumCost: EnchantmentRegistryEntry.EnchantmentCost = EnchantmentRegistryEntry.EnchantmentCost.of(5, 1),
    val activeSlots: EquipmentSlotGroup = EquipmentSlotGroup.MAINHAND,
    val inEnchantmentTable: Boolean = false
)

fun hasEnchantment(item: ItemStack, key: String): Pair<Boolean, Int?> {
    val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
    val meta = item.itemMeta ?: return Pair(false, null)
    val registryEnchant = registry.get(Key.key(key)) ?: return Pair(false, null)
    if (meta.hasEnchant(registryEnchant)) {
        return Pair(true, meta.getEnchantLevel(registryEnchant))
    }
    return Pair(false, null)

}