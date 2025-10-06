package dev.theWhiteBread.listeners.storage_system.controller.events

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerAccessEvent
import dev.theWhiteBread.menus.presets.storage_manager.ManagerMenu
import org.bukkit.event.EventHandler

object ControllerAccessListener : BreadListener {

    @EventHandler
    fun controllerAccess(event: StorageManagerAccessEvent) {
        val manager = event.manager
        if (manager.isThisAManager(event.player)) {
            ManagerMenu(event.player, manager).view.open()
        }
    }
}