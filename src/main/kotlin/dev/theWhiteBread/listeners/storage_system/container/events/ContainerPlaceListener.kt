package dev.theWhiteBread.listeners.storage_system.container.events

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerPlaceEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object ContainerPlaceListener : BreadListener {

    @EventHandler
    fun onContainerPlace(event: StorageContainerPlaceEvent) {
        if (event.canPlace) {
            event.container.saveToBlock()
            event.manager.containers.add(event.container)
            event.manager.save()
            event.player.message("<gray>You added a container to your managing storage chunk. Current container size: <gold>${event.manager.containers.size}")

        } else {
            event.parentEvent.isCancelled = true
            event.player.message("<gray>You are not allowed to modify this chunk.")
        }
    }
}