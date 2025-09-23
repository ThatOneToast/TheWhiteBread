package dev.theWhiteBread.listeners.storage_system.controller.events

import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerAccessEvent
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerBreakEvent
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerPlaceEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object Test : BreadListener {

    @EventHandler
    fun storageManagerPlaced(event: StorageManagerPlaceEvent) {
        if (!RateLimiter.tryExecuteIf3sPassed("${event.player.uniqueId}-storage_manager_timer") {
            event.player.message("<green>Placed manager")
            event.manager.load()

        }) {
            event.parentEvent.isCancelled = true
            event.player.message("<gray>Please wait at least 3 seconds before placing to avoid spam.")
        }

    }

    @EventHandler
    fun storageManagerBroken(event: StorageManagerBreakEvent) {
        event.player.message("<red>Broke manager")
        event.manager.unLoad()
    }

    @EventHandler
    fun onStorageManagerAccess(event: StorageManagerAccessEvent) {
        event.parentEvent.isCancelled = true
        event.player.message("<gray>Accessed event!!")
    }

}