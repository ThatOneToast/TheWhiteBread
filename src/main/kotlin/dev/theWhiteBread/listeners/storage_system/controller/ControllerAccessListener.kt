package dev.theWhiteBread.listeners.storage_system.controller

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.message
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.controller.StorageControllerMemberPermission
import dev.theWhiteBread.toUUID
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.LecternInventory

object ControllerAccessListener : BreadListener {

    @EventHandler
    fun storageControllerAccess(event: InventoryOpenEvent) {
        if (event.inventory !is LecternInventory) return
        val location = event.inventory.location ?: return

        if (!ControllerRegistry.isControllerCached(location.chunk)) {
            (event.player as Player).message("<red>No owner, or managers online. This Storage manager isn't cached.")
            return
        }

        val isController = ControllerRegistry.isController(location) ?: return
        event.isCancelled = true

        val members = isController.data.members.members.mapKeys { it.key.toUUID() }

        fun openByPerm(permission: StorageControllerMemberPermission) {
            when (permission) {
                StorageControllerMemberPermission.MANAGER -> isController.openViewAsManager(event.player as Player)
                StorageControllerMemberPermission.USER -> isController.openViewAsUser(event.player as Player)
                StorageControllerMemberPermission.VIEWER -> isController.openViewAsViewer(event.player as Player)
            }
        }

        if (isController.owner.toUUID() == event.player.uniqueId) {
            isController.cleanseChunkContainers()
            isController.view.open()
            return
        } else if (members.contains(event.player.uniqueId)) {
            isController.cleanseChunkContainers()
            openByPerm(members[event.player.uniqueId]!!)
            return
        } else if (isController.data.members.publicAccess.first) {
            val permission = isController.data.members.publicAccess.second ?: StorageControllerMemberPermission.VIEWER
            openByPerm(permission)
            return
        }

        (event.player as Player).message("<red>You are not the apart of this storage manager. bug off")

    }

}