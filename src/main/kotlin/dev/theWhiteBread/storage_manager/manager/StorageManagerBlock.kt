package dev.theWhiteBread.storage_manager.manager

import dev.theWhiteBread.PDC
import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.packInts
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.serializables.UUIDSerializer
import dev.theWhiteBread.storage_manager.StorageRegistry
import dev.theWhiteBread.storage_manager.StorageRegistry.StorageManagers
import dev.theWhiteBread.storage_manager.manager.members.MemberPermission
import dev.theWhiteBread.storage_manager.manager.members.StorageManagerMember
import dev.theWhiteBread.toSerLocation
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.*

@Serializable
data class StorageManagerBlockData(
    val location: SerializableLocation,
    val ownerUUID: @Serializable(with = UUIDSerializer::class) UUID,
    val members: List<StorageManagerMember>,
    val id: @Serializable(with = UUIDSerializer::class) UUID = UUID.randomUUID()
) {

    init {

    }

    fun toManagerBlock(loadManager: Boolean = false, computeInManager: Boolean = false): StorageManagerBlock {
        val block = StorageManagerBlock(
            Bukkit.getOfflinePlayer(ownerUUID),
            location.toBlock(),
            members.toHashSet(),
            id
        )
        if (loadManager) {
            StorageRegistry.loadedManagers[location.chunkKey] = block
        }
        if (computeInManager) {
            StorageRegistry.storageManagers.storageManagers.computeIfAbsent(block.block.world.uid) {hashMapOf()}
            StorageRegistry.storageManagers.storageManagers[block.block.world.uid]!![block.block.location.chunkKey()] = block.toManagerBlockData(unloadManager = false)
        }
        return block
    }
}

class StorageManagerBlock (
    val owner: OfflinePlayer,
    val block: Block,
    val members: HashSet<StorageManagerMember>,
    val id: UUID
) {

    init {

    }


    fun getOnlineManager(): Player? {
        val owner = Bukkit.getPlayer(owner.uniqueId)
        if (owner != null) return owner
        return members.filter {
            it.permission.isAtLeast(MemberPermission.Manager)
        }.first {
            it.member.isOnline
        }.member.player
    }

    fun getOwner(): Player? = Bukkit.getPlayer(owner.uniqueId)

    fun toManagerBlockData(unloadManager: Boolean = false): StorageManagerBlockData {
        val blockData = StorageManagerBlockData(
            block.location.toSerLocation(),
            owner.uniqueId,
            members.toList(),
            id
        )
        if (unloadManager) {
            StorageRegistry.loadedManagers.remove(packInts(block.location.chunk.x, block.location.chunk.z))
        }
        return blockData
    }

    fun unLoad() {
        StorageRegistry.storageManagers.storageManagers.get(block.location.world.uid)?.set(block.location.chunkKey(), this.toManagerBlockData())
        StorageRegistry.loadedManagers.remove(block.location.chunkKey())
    }

    fun load() {
        StorageRegistry.storageManagers.storageManagers.computeIfAbsent(block.world.uid) {
            hashMapOf(block.location.chunkKey() to this.toManagerBlockData(unloadManager = false))
        }
        StorageRegistry.loadedManagers[block.location.chunkKey()] = this
    }
}

