package dev.theWhiteBread.listeners.storage_system.controller.events

import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerPlaceEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object ControllerPlaceListener : BreadListener {

    @EventHandler
    fun controllerPlaced(event: StorageManagerPlaceEvent) {
        val ran = RateLimiter.tryExecuteIf3sPassed("${event.player.uniqueId}-controller-placement") {
            if (event.couldPlace) {
                event.manager.load()
                event.player.message("<gray>Placed a storage manager.")
            } else {
                event.parentEvent.isCancelled = true
                val owningManager = event.manager
                event.player.message("<gold>${owningManager.owner.name}</gold><gray> Owns this chunk. You are not allowed to do this.")
            }
        }

        if (!ran) {
            event.parentEvent.isCancelled = true
            event.player.message("<gray>Please wait at least <gold>3</gold> before placement again")
        }

    }

}