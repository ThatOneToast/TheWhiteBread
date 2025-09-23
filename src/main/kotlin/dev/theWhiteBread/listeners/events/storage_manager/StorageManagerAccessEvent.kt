package dev.theWhiteBread.listeners.events.storage_manager

import dev.theWhiteBread.listeners.BaseEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.menus.presets.storage_manager.ManagerMenu
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerInteractEvent

class StorageManagerAccessEvent(
    val player: Player,
    val manager: StorageManagerBlock,
    val view: ManagerMenu,
    val parentEvent: PlayerInteractEvent
    ): BaseEvent() {

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(StorageManagerAccessEvent::class.java)
    }
}