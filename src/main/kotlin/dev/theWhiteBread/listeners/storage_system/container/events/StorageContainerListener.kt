package dev.theWhiteBread.listeners.storage_system.container.events

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerAccessEvent
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerBreakEvent
import dev.theWhiteBread.listeners.events.storage_manager.container.StorageContainerPlaceEvent
import dev.theWhiteBread.message
import dev.theWhiteBread.storage_manager.container.StorageContainer
import dev.theWhiteBread.parseLocation
import dev.theWhiteBread.storage_manager.StorageRegistry
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

object StorageContainerListener : BreadListener {

    @EventHandler
    fun storageContainerAccess(event: PlayerInteractEvent) {
        val storageContainer = StorageContainer.fromBlock(event.clickedBlock ?: return) ?: return
        TheWhiteBread.instance.server.pluginManager.callEvent(StorageContainerAccessEvent(
            event.player,
            storageContainer,
            StorageRegistry.storageManagers(event.player.world.uid)[event.player.location.chunkKey()] ?: return,
            parentEvent = event

        ))
    }

    @EventHandler
    fun storageContainerPlace(event: BlockPlaceEvent) {
        val blockState = event.block.state as? TileState ?: return
        if (!event.itemInHand.itemMeta.persistentDataContainer.has(Keys.create("storage_container"))) return
        val storageManager = StorageRegistry.storageManagers(event.player.world.uid)[event.player.location.chunkKey()] ?: return


        val storageContainer = StorageContainer.new(blockState.location.chunkKey(), event.block)
        val isManager = storageManager.isThisAManager(event.player)

        TheWhiteBread.instance.server.pluginManager.callEvent(StorageContainerPlaceEvent(
            event.player,
            canPlace = isManager,
            storageContainer,
            storageManager,
            parentEvent = event
        ))

    }

    @EventHandler
    fun storageContainerBreak(event: BlockBreakEvent) {
        val storageContainer = StorageContainer.fromBlock(event.block) ?: return
        val storageManager = storageContainer.getManagerBlock()
        val isManager = storageManager.isThisAManager(event.player)

        TheWhiteBread.instance.server.pluginManager.callEvent(StorageContainerBreakEvent(
            event.player,
            isManager,
            storageContainer,
            storageManager,
            event
        ))

    }

}