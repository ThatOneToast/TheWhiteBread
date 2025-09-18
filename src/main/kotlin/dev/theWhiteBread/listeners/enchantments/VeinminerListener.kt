package dev.theWhiteBread.listeners.enchantments

import dev.theWhiteBread.enchantments.enchantment.Veinminer
import dev.theWhiteBread.enchantments.hasEnchantment
import dev.theWhiteBread.listeners.BreadListener
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent

object VeinminerListener : BreadListener {

    @EventHandler
    fun veinMiner(event: BlockBreakEvent) {
        val player = event.player

        val item = player.inventory.itemInMainHand
        val check = hasEnchantment(item, "whitebread:veinminer")

        if (check.first) {
            val blocks = Veinminer.veinMine(player, event.block, check.second!!) ?: return
            if (blocks.size <= 5) return
            val hungerToRemove = blocks.size / 20
            val dirrPoints = blocks.size / 5
            player.foodLevel -= hungerToRemove
            item.damage(dirrPoints, player)
        }


    }
}