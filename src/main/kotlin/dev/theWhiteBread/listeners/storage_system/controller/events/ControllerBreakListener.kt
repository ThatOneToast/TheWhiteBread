package dev.theWhiteBread.listeners.storage_system.controller.events

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerBreakEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object ControllerBreakListener : BreadListener {

    @EventHandler
    fun onControllerBreak(event: StorageManagerBreakEvent) {
        if (event.manager.isThisAManager(event.player)) {
            event.manager.remove()
            event.player.message("<gray>You have <red>removed</red> <gold>${event.manager.owner.name}'s</gold> storage controller")
        } else {
            event.parentEvent.isCancelled = true
            event.player.message("<red>You do not have the permissions needed to remove <gold>${event.manager.owner.name}'s</gold> storage controller")

        }
    }
}