package dev.theWhiteBread.listeners.storage_system.container.events

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerAccessEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object ContainerAccessListener : BreadListener {

    @EventHandler
    fun onOpenBarrel(event: StorageContainerAccessEvent) {
        if (event.container.rawLocked) {
            if (!event.manager.isThisAManager(event.player)) {
                event.player.message("<gray>This container is locked by a mystical force.")
                event.parentEvent.isCancelled = true
            }
        }
    }
}