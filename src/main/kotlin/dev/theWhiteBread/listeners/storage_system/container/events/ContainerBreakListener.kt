package dev.theWhiteBread.listeners.storage_system.container.events

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerBreakEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object ContainerBreakListener : BreadListener {

    @EventHandler
    fun onContainerBreak(event: StorageContainerBreakEvent) {
        event.player.message("<gray>Break container event fired.")
        if (event.canBreak) {
            event.container.remove(false)
            event.player.message("<gray>You have removed a container from your owning chunk storage land. Container size: <gold>${event.manager.containers.size}")
        } else {
            event.parentEvent.isCancelled = true
            event.player.message("<red>You are not allowed to break container within this owning chunk. Get required permissions.")
        }
    }
}