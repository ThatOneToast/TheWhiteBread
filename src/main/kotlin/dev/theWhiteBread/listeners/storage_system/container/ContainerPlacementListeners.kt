package dev.theWhiteBread.listeners.storage_system.container

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.message
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.controller.StorageController
import dev.theWhiteBread.storage_system.controller.StorageControllerMemberPermission
import dev.theWhiteBread.toUUID
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.TileState
import org.bukkit.block.data.type.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

object ContainerPlacementListeners : BreadListener {
    @EventHandler
    fun storageContainerBroken(event: BlockBreakEvent) {
        val block = event.block
        val blockState = block.state as? TileState ?: return
        val player = event.player

        val containerInfo = blockState.persistentDataContainer.get(Keys.itemStorageContainer, PDC.chunkStorageContainer) ?: return
        val controllerLOC = containerInfo.parentControllerLocation.fromSerLocation()
        val controller = ControllerRegistry.getController(controllerLOC.chunk) ?: return

        if (player.uniqueId == controller.owner.toUUID() ||
            controller.data.members.members.filter { it.value == StorageControllerMemberPermission.MANAGER }.contains(player.uniqueId.toString())
        ) {
            controller.removeContainer(block)
            blockState.update(true, false)
            controller.updateViewState()
            controller.save()
        } else {
            player.message("<red>This is another player's storage chunk, this container will not be destroyed.")
            event.isCancelled = true
            return
        }


    }

    @EventHandler
    fun storageContainerPlaced(event: BlockPlaceEvent) {
        val block = event.block
        val player = event.player
        if (block.state !is Container) return
        val controller = StorageController.findStorageController(block.chunk) ?: return


        if (player.uniqueId == controller.owner.toUUID() ||
            controller.data.members.members.filter { it.value == StorageControllerMemberPermission.MANAGER }.contains(player.uniqueId.toString())
        ) {
            RateLimiter.runTicksLater(3) {
                val world = Bukkit.getWorld(block.location.world.uid)
                if (world == null) {
                    TheWhiteBread.pluginLogger.warning("World not loaded for ${block.location}")
                    return@runTicksLater
                }

                val placed = world.getBlockAt(block.location)
                val placedData = placed.blockData as? Chest

                controller.addContainer(placed.location)

                val faces = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
                for (face in faces) {
                    val nb = placed.getRelative(face)
                    if (nb.chunk != placed.chunk) continue
                    if (nb.state is Container) controller.addContainer(nb.location)
                }

                controller.cleanseChunkContainers()
                controller.updateViewState()
                controller.save()
            }
        } else {
            player.message("<red>This is another player's storage chunk, your container will not be added.")
            event.isCancelled = true
            return
        }

    }
}