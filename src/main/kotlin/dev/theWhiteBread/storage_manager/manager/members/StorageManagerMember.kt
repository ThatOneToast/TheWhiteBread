package dev.theWhiteBread.storage_manager.manager.members

import kotlinx.serialization.Serializable
import org.bukkit.OfflinePlayer

@Serializable
data class StorageManagerMember(
    val member: OfflinePlayer,
    val permission: MemberPermission
    ) {

}