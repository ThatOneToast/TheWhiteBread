package dev.theWhiteBread.listeners.storage_system.controller

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerAccessEvent
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerBreakEvent
import dev.theWhiteBread.listeners.events.storage_manager.StorageManagerPlaceEvent
import dev.theWhiteBread.menus.presets.storage_manager.ManagerMenu
import dev.theWhiteBread.message
import dev.theWhiteBread.packInts
import dev.theWhiteBread.storage_manager.StorageRegistry
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlockData
import dev.theWhiteBread.toSerLocation
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataType

object SMEventCallerListener : BreadListener {
    private var slowStartUpTicks = 20

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        if (event.world.name == "world") {
            StorageRegistry.init(TheWhiteBread.instance)
        }
    }

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        if (event.world.name == "world") {
            StorageRegistry.storageManagers.save()
        }
    }

    @EventHandler
    fun onControllerLoad(event: ChunkLoadEvent) {
        if (slowStartUpTicks != 0) {
            RateLimiter.runTicksLater(slowStartUpTicks) {
                val chunkKey = packInts(event.chunk.x, event.chunk.z)
                if (!StorageRegistry.loadedManagers.contains(chunkKey)) {
                    val sm = StorageRegistry.storageManagers.storageManagers.get(event.world.uid)?.get(chunkKey) ?: return@runTicksLater
                    sm.toManagerBlock(true)
                }
            }
            return
        }
        val chunkKey = packInts(event.chunk.x, event.chunk.z)
        if (!StorageRegistry.loadedManagers.contains(chunkKey)) {
            val sm = StorageRegistry.storageManagers.storageManagers.get(event.world.uid)?.get(chunkKey) ?: return
            sm.toManagerBlock(true)
        }
    }

    @EventHandler
    fun onControllerUnload(event: ChunkUnloadEvent) {
        if (slowStartUpTicks != 0) {
            RateLimiter.runTicksLater(slowStartUpTicks) {
                val chunkKey = packInts(event.chunk.x, event.chunk.z)
                StorageRegistry.loadedManagers[chunkKey]?.unLoad() ?: return@runTicksLater
            }
            return
        }
        val chunkKey = packInts(event.chunk.x, event.chunk.z)
        StorageRegistry.loadedManagers[chunkKey]?.unLoad() ?: return
    }


    @EventHandler
    fun onControllerPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        PDC.valueOf<Boolean>(item.itemMeta.persistentDataContainer, Keys.storageController) ?: return

        val block = event.blockPlaced
        val blockState = block.state as? TileState ?: throw IllegalStateException("\"Storage Controller\" isn't a tile state block.")
        val manager = StorageRegistry.loadedManagers[block.location.chunkKey()]

        if (manager == null) {
            PDC.setValueOf(blockState.persistentDataContainer, Keys.storageController, true)
            blockState.update(true, false)

            val storageControllerData = StorageManagerBlockData(
                block.location.toSerLocation(),
                event.player.uniqueId,
                emptyList(),
            )
            val storageControllerBlock = storageControllerData.toManagerBlock(loadManager = false, computeInManager = true)
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
        val storageManager = StorageRegistry.storageManagers.storageManagers[block.location.world.uid]!![block.location.chunkKey()]
        if (storageManager == null) {
            TheWhiteBread.pluginLogger.error("[SM] Storage Manager block had key of being a storage manager.\nWasn't registered. It's possible this storage manager was deregistered before it was broken.")
            return
        }
        TheWhiteBread.instance.server.pluginManager.callEvent(
            StorageManagerBreakEvent(
                event.player,
                manager = storageManager.toManagerBlock(loadManager = false),
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
            val manager = StorageRegistry.loadedManagers[block.location.chunkKey()] ?: throw IllegalStateException("[SM] Storage manager doesn't exist despite having a key saying so.")
            TheWhiteBread.instance.server.pluginManager.callEvent(
                StorageManagerAccessEvent(
                    event.player,
                    manager,
                    view = ManagerMenu(event.player),
                    parentEvent = event
                )
            )
        }
    }
}