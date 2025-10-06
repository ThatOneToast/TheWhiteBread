package dev.theWhiteBread.storage_manager

import dev.theWhiteBread.PDC
import dev.theWhiteBread.serializables.UUIDSerializer
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlockData
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

typealias WorldUUID = @Serializable(with = UUIDSerializer::class) UUID

object StorageRegistry {

    /**
     * Access via worldUUID,
     */
    val storageManagers: (UUID) -> HashMap<Long, StorageManagerBlock> = { worldUUID ->
        val world = Bukkit.getWorld(worldUUID) ?: throw IllegalArgumentException("Invalid WORLD UUID")
        val managers = PDC.getOrCompute<HashMap<Long, StorageManagerBlockData>>(world.persistentDataContainer, "storage_manager_collection", hashMapOf())

        val map = hashMapOf<Long, StorageManagerBlock>()
        managers.forEach {
            map[it.key] = it.value.toManagerBlock()
        }
        map
    }

    fun saveStorageManager(worldUUID: UUID, chunkKey: Long, manager: StorageManagerBlock) {
        removeManager(worldUUID, chunkKey)
        addManager(worldUUID, chunkKey, manager.toManagerBlockData())
    }

    /**
     * Add a manager to persistent storage.
     *
     * return true if added successfully
     * returns false if a manager already exists within that world:chunk pair.
     */
    fun addManager(worldUUID: UUID, chunkKey: Long, manager: StorageManagerBlockData): Boolean {
        val world = Bukkit.getWorld(worldUUID) ?: throw IllegalArgumentException("Invalid WORLD UUID")
        val managers = PDC.getOrCompute<HashMap<Long, StorageManagerBlockData>>(world.persistentDataContainer, "storage_manager_collection", hashMapOf())
        if (managers.contains(chunkKey)) return false
        managers[chunkKey] = manager
        PDC.setValueOf(world.persistentDataContainer, "storage_manager_collection", managers)
        return true
    }

    fun removeManager(worldUUID: UUID, chunkKey: Long) {
        val world = Bukkit.getWorld(worldUUID) ?: throw IllegalArgumentException("Invalid WORLD UUID")
        val managers = PDC.getOrCompute<HashMap<Long, StorageManagerBlockData>>(world.persistentDataContainer, "storage_manager_collection", hashMapOf())
        managers.remove(chunkKey)
        PDC.setValueOf(world.persistentDataContainer, "storage_manager_collection", managers)
    }




}