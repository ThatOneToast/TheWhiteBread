package dev.theWhiteBread.listeners.events.storage_manager

import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.listeners.BaseEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockPlaceEvent

/**
 * @param player - The player that triggered the event
 * @param manager - The manager involved in the chunk. If there was a manager already placed within a chunk,
 * the manager of that chunk is returned. Not the players placed manager.
 * @param couldPlace - If the player successfully placed their storage manager.
 * If false a storage manager already exists, and manager will reflect that manager.
 */
class StorageManagerPlaceEvent(
    val player: Player,
    val manager: StorageManagerBlock,
    val couldPlace: Boolean,
    val parentEvent: BlockPlaceEvent
) :  BaseEvent()
{

    /**
     * Sets manager to air
     * returns item back to player
     *
     * **Note**: If you loaded the manager into the registry, you must your self unload it.
     */
    fun cancelManagerPlacement() {
        manager.block.setType(Material.AIR, false)
        player.inventory.addItem(ItemRegistry.storageController.clone())
    }

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(StorageManagerPlaceEvent::class.java)
    }
}