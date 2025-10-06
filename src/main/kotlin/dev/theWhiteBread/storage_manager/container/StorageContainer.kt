package dev.theWhiteBread.storage_manager.container

import dev.theWhiteBread.PDC
import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.packInts
import dev.theWhiteBread.serializables.InventorySerializer
import dev.theWhiteBread.serializables.LocationSerializer
import dev.theWhiteBread.storage_manager.StorageRegistry
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

@Serializable
data class StorageContainer(
    val owner: Long,
    @Serializable(with = InventorySerializer::class)
    var inventory: Inventory,
    @Serializable(with = LocationSerializer::class)
    val location: Location,
    var rawLocked: Boolean = false
) {
    val block = location.block

    fun open(player: Player) {
        player.openInventory(inventory)
    }

    fun saveToBlock() {
        val blockState = block.state as TileState
        PDC.setValueOf<StorageContainer>(blockState.persistentDataContainer, "storage_container", this)
        blockState.update(true, false)
    }

    /**
     * Delete this storage instance,
     * Will remove from storage manager
     */
    fun remove(retainOrigBlock: Boolean = false) {
        val manager = getManagerBlock()
        if (!retainOrigBlock) {
            val state = block.state as TileState
            state.type = Material.AIR
            PDC.deleteData(state.persistentDataContainer, "storage_container")
            state.update(true, true)
        } else {
            val state = block.state as TileState
            PDC.deleteData(state.persistentDataContainer, "storage_container")
            state.update(true, false)
        }
        manager.containers.remove(this)
        manager.save()
    }

    fun getManagerBlock(): StorageManagerBlock {
        return StorageRegistry.storageManagers(location.world.uid)[location.chunkKey()]!!
    }



    companion object {
        fun new(owner: Chunk, containerBlock: Block): StorageContainer {
           return new(packInts(owner.x, owner.z), containerBlock)
        }

        fun new(owner: Long, containerBlock: Block): StorageContainer {
            return StorageContainer(
                owner,
                Bukkit.createInventory(null, 54),
                containerBlock.location
            )
        }

        fun fromBlock(block: Block): StorageContainer? {
            val blockState = block.state as? TileState ?: return null
            return PDC.valueOf<StorageContainer>(blockState.persistentDataContainer, "storage_container")
        }
    }
}