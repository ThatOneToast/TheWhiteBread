package dev.theWhiteBread.listeners.enchantments

import dev.theWhiteBread.enchantments.enchantment.EXPBoost
import dev.theWhiteBread.enchantments.hasEnchantment
import dev.theWhiteBread.listeners.BreadListener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerExpChangeEvent

object EXPBoostListener : BreadListener {

    @EventHandler
    fun playerEXPChange(event: PlayerExpChangeEvent) {
        val enchantment = hasEnchantment((event.player.inventory.leggings ?: return), "whitebread:expboost")
        if (!enchantment.first) return
        if (enchantment.second!! <= 0) return
        event.amount = EXPBoost.applyExpBoost(event.amount, enchantment.second!!)
    }

}