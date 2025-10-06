package dev.theWhiteBread.listeners.events.storage_manager.container

import dev.theWhiteBread.listeners.BaseEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.storage_manager.container.StorageContainer
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockBreakEvent

class StorageContainerBreakEvent(
    val player: Player,
    val canBreak: Boolean,
    val container: StorageContainer,
    val manager: StorageManagerBlock,
    val parentEvent: BlockBreakEvent
): BaseEvent() {

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(StorageContainerBreakEvent::class.java)
    }
}