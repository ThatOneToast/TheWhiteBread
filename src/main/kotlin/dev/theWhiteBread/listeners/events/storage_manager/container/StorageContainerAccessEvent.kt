package dev.theWhiteBread.listeners.events.storage_manager.container

import dev.theWhiteBread.listeners.BaseEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.storage_manager.container.StorageContainer
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerInteractEvent

class StorageContainerAccessEvent(
    val player: Player,
    val container: StorageContainer,
    val manager: StorageManagerBlock,
    val parentEvent: PlayerInteractEvent
): BaseEvent() {

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(StorageContainerAccessEvent::class.java)
    }
}