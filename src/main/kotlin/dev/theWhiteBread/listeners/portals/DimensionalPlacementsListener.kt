package dev.theWhiteBread.listeners.portals

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.input.ChatInput
import dev.theWhiteBread.message
import dev.theWhiteBread.portals.DimensionalReceiver
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.portals.portal.UnstableBreachPortal
import dev.theWhiteBread.toSerLocation
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.util.Vector

object DimensionalPlacementsListener : BreadListener {

    @EventHandler
    fun onDimensialReceiverPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (item != ItemRegistry.dimensionalReceiver) return
        val blockState = event.block.state as TileState
        event.player.message("<gray>Please type the <green>identifier</green> for this breach receiver.")
        ChatInput.registerNewInputListener(event.player.uniqueId) { player, id -> run { // This is a async thread
            player.message("<gray>Portaled ID as <green>$id</green>")
            RateLimiter.runTicksLater(1) { // Doing this to get back into the main thread.
                val dimensialData = DimensionalReceiver(blockState.location.add(Vector(0, 3, 0)).toSerLocation(), blockState.location.toSerLocation(),"red", id)
                blockState.persistentDataContainer.set(Keys.dimensialReceiver, PDC.dimensialReceiver, dimensialData)
                blockState.update(true, false)
                val unstablePortal =
                    UnstableBreachPortal(id, player.uniqueId.toString(), dimensialData.location.fromSerLocation())
                PortalManager.registerPortal(unstablePortal, true)
            }
            ChatInput.finishInputListening(player.uniqueId)
        }}

    }

    @EventHandler
    fun onDimensialReceiverBreak(event: BlockBreakEvent) {
        val block = event.block.state as? TileState ?: return
        val dimensialData = block.persistentDataContainer.get(Keys.dimensialReceiver, PDC.dimensialReceiver) ?: return
        val portalID = dimensialData.id
        PortalManager.deRegisterPortal(portalID, fromPersistence = true)
    }

}