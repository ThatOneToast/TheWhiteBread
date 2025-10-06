package dev.theWhiteBread.listeners.storage_system.controller.events

import dev.theWhiteBread.*
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerAccessEvent
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerBreakEvent
import dev.theWhiteBread.listeners.events.storage_manager.manager.StorageManagerPlaceEvent
import dev.theWhiteBread.storage_manager.StorageRegistry
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlockData
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

object StorageManagerListener : BreadListener {
    private var slowStartUpTicks = 20


    @EventHandler
    fun onControllerPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        PDC.valueOf<Boolean>(item.itemMeta.persistentDataContainer, Keys.storageController) ?: return

        val block = event.blockPlaced
        val blockState = block.state as? TileState ?: throw IllegalStateException("\"Storage Controller\" isn't a tile state block.")
        val manager = StorageRegistry.storageManagers(event.player.world.uid)[event.player.location.chunkKey()]

        if (manager == null) {
            PDC.setValueOf(blockState.persistentDataContainer, Keys.storageController, true)
            blockState.update(true, false)

            val storageControllerData = StorageManagerBlockData(
                block.location.toSerLocation(),
                event.player.uniqueId,
                emptyList(),
                hashSetOf()
            )
            val storageControllerBlock = storageControllerData.toManagerBlock(loadManager = false)
            TheWhiteBread.instance.server.pluginManager.callEvent(
                StorageManagerPlaceEvent(
                    event.player,
                    storageControllerBlock,
                    couldPlace = true,
                    parentEvent = event
                )
            )

        } else {
            TheWhiteBread.instance.server.pluginManager.callEvent(
                StorageManagerPlaceEvent(
                    event.player,
                    manager,
                    couldPlace = false,
                    parentEvent = event
                )
            )

        }
    }

    @EventHandler
    fun onControllerBreak(event: BlockBreakEvent) {
        val block = event.block
        val blockState = block.state as? TileState ?: return
        PDC.valueOf<Boolean>(blockState.persistentDataContainer, Keys.storageController) ?: return
        val storageManager = StorageRegistry.storageManagers(event.player.world.uid)[block.location.chunkKey()]
        if (storageManager == null) {
            TheWhiteBread.pluginLogger.error("[SM] Storage Manager block had key of being a storage manager.\nWasn't registered. It's possible this storage manager was deregistered before it was broken.")
            return
        }
        TheWhiteBread.instance.server.pluginManager.callEvent(
            StorageManagerBreakEvent(
                event.player,
                manager = storageManager,
                parentEvent = event
            )
        )
    }

    @EventHandler
    fun onControllerAccess(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            val blockState = block.state as? TileState ?: return
            PDC.valueOf<Boolean>(blockState.persistentDataContainer, Keys.storageController) ?: return
            val manager = StorageRegistry.storageManagers(event.player.world.uid)[block.location.chunkKey()] ?: throw IllegalStateException("[SM] Storage manager doesn't exist despite having a key saying so.")
            event.isCancelled = true
            TheWhiteBread.instance.server.pluginManager.callEvent(
                StorageManagerAccessEvent(
                    event.player,
                    manager,
                    parentEvent = event
                )
            )
        }
    }

}