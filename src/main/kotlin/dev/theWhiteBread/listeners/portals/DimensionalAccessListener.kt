package dev.theWhiteBread.listeners.portals

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.menus.presets.PortalMenus
import dev.theWhiteBread.message
import org.bukkit.block.TileState
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryOpenEvent

object DimensionalAccessListener : BreadListener {

    @EventHandler
    fun onDimensialReceiverAccess(event: InventoryOpenEvent) {
        val player = event.player as Player
        val block = event.inventory.location?.block?.state as? TileState ?: return
        player.message("accessing receiver block state")
        val data = block.persistentDataContainer.get(Keys.dimensialReceiver, PDC.dimensialReceiver) ?: return
        player.message("accessing receiver block state x2")
        event.isCancelled = true
        PortalMenus.dimensialReceiverOptions(player, data).open()

    }
}