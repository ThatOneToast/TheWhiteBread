package dev.theWhiteBread.storage_manager

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.serializables.UUIDSerializer
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlockData
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

typealias WorldUUID = @Serializable(with = UUIDSerializer::class) UUID

object StorageRegistry {
    lateinit var storageManagers: StorageManagers
    val loadedManagers: HashMap<Long, StorageManagerBlock> = hashMapOf()

    fun init(plugin: JavaPlugin) {
        val world = plugin.server.getWorld("world")
            ?: plugin.server.worlds.firstOrNull()
            ?: throw IllegalStateException("No worlds loaded")
        storageManagers = PDC.getOrCompute(
            world.persistentDataContainer,
            "storage_managers",
            StorageManagers(storageManagers = hashMapOf())
        )
    }

    @Serializable
    data class StorageManagers (
        val storageManagers: HashMap<WorldUUID, HashMap<Long, StorageManagerBlockData>>
    ) {

        fun fromWorld(worldUUID: WorldUUID): HashMap<Long, StorageManagerBlockData>? {
            return storageManagers[worldUUID]
        }

        fun getManager(worldUUID: WorldUUID, packedChunkKey: Long): StorageManagerBlockData? {
            return storageManagers[worldUUID]?.get(packedChunkKey)
        }

        fun setManager(worldUUID: WorldUUID, packedChunkKey: Long, data: StorageManagerBlockData) {
            storageManagers[worldUUID]?.set(packedChunkKey, data)
        }


        fun save() {
            val world = Bukkit.getWorld("world")!!
            PDC.setValueOf(world.persistentDataContainer, "storage_managers", this)
        }



    }

}