package dev.theWhiteBread.storage_manager.manager

import dev.theWhiteBread.PDC
import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.storage_manager.container.StorageContainer
import dev.theWhiteBread.packInts
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.serializables.UUIDSerializer
import dev.theWhiteBread.storage_manager.StorageRegistry
import dev.theWhiteBread.storage_manager.manager.members.MemberPermission
import dev.theWhiteBread.storage_manager.manager.members.StorageManagerMember
import dev.theWhiteBread.toSerLocation
import dev.theWhiteBread.toUUID
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
    val containers: HashSet<StorageContainer>,
    val id: @Serializable(with = UUIDSerializer::class) UUID = UUID.randomUUID(),
) {

    fun toManagerBlock(loadManager: Boolean = false): StorageManagerBlock {
        val block = StorageManagerBlock(
            Bukkit.getOfflinePlayer(ownerUUID),
            location.toBlock(),
            members.toHashSet(),
            containers,
            id
        )
        if (loadManager) StorageRegistry.addManager(location.world.toUUID(), location.chunkKey, this)
        return block
    }
}

class StorageManagerBlock (
    val owner: OfflinePlayer,
    val block: Block,
    val members: HashSet<StorageManagerMember>,
    val containers: HashSet<StorageContainer>,
    val id: UUID
) {

    init {

    }

    fun isThisAManager(player: Player): Boolean = getOnlineManagers().map { it.uniqueId }.contains(player.uniqueId)

    fun getOnlineManager(): Player? {
        val owner = Bukkit.getPlayer(owner.uniqueId)
        if (owner != null) return owner
        return members.filter {
            it.permission.isAtLeast(MemberPermission.Manager)
        }.first {
            it.member.isOnline
        }.member.player
    }

    fun getOnlineManagers(): HashSet<Player> {
        val onlineMembers = mutableSetOf<Player>()
        onlineMembers.addAll(members.filter {
            it.member.isOnline && it.permission.isAtLeast(MemberPermission.Manager)
        }.map { it.member.player!! })
        owner.player?.apply {
            onlineMembers.add(this)
        }
        return onlineMembers.toHashSet()
    }

    fun getOwner(): Player? = Bukkit.getPlayer(owner.uniqueId)

    fun toManagerBlockData(): StorageManagerBlockData {
        val blockData = StorageManagerBlockData(
            block.location.toSerLocation(),
            owner.uniqueId,
            members.toList(),
            containers,
            id
            )
        return blockData
    }

    fun remove() {
        StorageRegistry.removeManager(block.location.world.uid, block.location.chunkKey())
    }

    fun save() {
        StorageRegistry.saveStorageManager(block.location.world.uid, block.location.chunkKey(), this)
    }

    fun load() {
        StorageRegistry.addManager(block.location.world.uid, block.location.chunkKey(), this.toManagerBlockData())
    }
}

