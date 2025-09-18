package dev.theWhiteBread.listeners.enchantments

import dev.theWhiteBread.enchantments.enchantment.AutoSmelt
import dev.theWhiteBread.enchantments.hasEnchantment
import dev.theWhiteBread.listeners.BreadListener
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent

object AutoSmeltListener : BreadListener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreakAutoSmelt(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand

        if (!hasEnchantment(tool, "whitebread:autosmelt").first) return

        // If player has veinminer on the tool, let veinminer handle multi-block drops
        if (hasEnchantment(tool, "whitebread:veinminer").first) return

        val smelt = AutoSmelt.getAutoSmeltedDrops(event.block, tool)
        if (smelt.isEmpty()) return

        event.isDropItems = false
        smelt.forEach {
            event.block.world.dropItemNaturally(event.block.location, it)
            AutoSmelt.spawnExperienceOrb(event.block.location, AutoSmelt.vanillaXpForBlock(event.block, tool))
        }
        event.block.type = Material.AIR

    }
}