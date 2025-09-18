package dev.theWhiteBread.listeners.storage_system.controller

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.message
import dev.theWhiteBread.miniMessage
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.controller.StorageControllerData
import dev.theWhiteBread.storage_system.controller.StorageControllerMembers
import dev.theWhiteBread.toSerLocation
import dev.theWhiteBread.toSerializedLocationMap
import dev.theWhiteBread.toUUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

object ControllerPlacementsListener : BreadListener {

    @EventHandler
    fun storageControllerPlacement(event: BlockPlaceEvent) {
        val block = event.block
        val controller = event.itemInHand
        if (controller != ItemRegistry.storageController) return

        val controllerList = event.player.persistentDataContainer.get(Keys.playerStorageControllers, PDC.playerStorageControllerList) ?: listOf()
        if (controllerList.size > 1) {
            event.player.message("<red>You are at the maximum allotted storage manager.")
            event.isCancelled = true
            return
        }

        val controllerInfo = ControllerRegistry.addControllerOrNull(event.player, block)

        /*
        There is already a controller in this chunk. Warning the player they cannot place theirs here.
         */
        if (controllerInfo == null) {
            event.isCancelled = true
            val chunkOwnerName = Bukkit.getPlayer(ControllerRegistry.getControllerByChunk(block.chunk)!!.owner.toUUID())!!.name
            event.player.message("<red>There is already a storage controller in this chunk.\n<bold>Owner: $chunkOwnerName</bold>\nSelect a different chunk for your manager.")
            return
        }

        val didRun = RateLimiter.tryExecuteIf60sPassed(event.player.uniqueId.toString()) {
            controllerInfo.scanChunkReplaceMemoryContainers()
            controllerInfo.updateViewState()

            val state = block.state as TileState
            val containers = controllerInfo.memoryChunkContainers.second
            val serializedContainers = containers.toSerializedLocationMap()

            state.persistentDataContainer.set(Keys.itemStorageController, PDC.storageControllerData,
                StorageControllerData(
                    event.player.uniqueId.toString(),
                    state.location.toSerLocation(),
                    serializedContainers,
                    StorageControllerMembers(),
                    false,
                    mutableSetOf()
                )
            )
            state.update(true)

            val playerControllerList = event.player.persistentDataContainer.get(Keys.playerStorageControllers,
                PDC.playerStorageControllerList)?.toMutableList() ?: mutableListOf()
            playerControllerList.add(controllerInfo.block.location.toSerLocation())
            event.player.persistentDataContainer.set(Keys.playerStorageControllers, PDC.playerStorageControllerList, playerControllerList)
            event.player.message("<green>Placed storage controller")
        }

        if (!didRun) {
            event.player.message("<red>Please wait at least 1 minute before placing another storage controller.")
            event.isCancelled = true
        }

    }

    @EventHandler
    fun storageControllerBroken(event: BlockBreakEvent) {
        val block = event.block
        if (block.type != Material.LECTERN) return
        val controller = ControllerRegistry.isControllerPrecise(block.location)
        if (controller != null ) {
            if (controller.owner.toUUID() == event.player.uniqueId) {
                val controllers = event.player.persistentDataContainer.get(Keys.playerStorageControllers,
                    PDC.playerStorageControllerList)!!.toMutableList()
                val kill = controllers.find{ it.fromSerLocation() == controller.block.location}!!
                controllers.remove(kill)
                event.player.persistentDataContainer.set(Keys.playerStorageControllers, PDC.playerStorageControllerList, controllers)
                ControllerRegistry.removeController(block.location)
                event.player.sendMessage(miniMessage.deserialize("<red>Storage controller broken."))
                block.type = Material.AIR
                event.isCancelled = true
                event.player.inventory.addItem(ItemRegistry.storageController)
                event.player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK))
            } else {
                event.player.sendMessage(miniMessage.deserialize("<red>You do not own this storage controller therefore you cannot break it."))
                event.isCancelled = true
            }
        }
    }

}