package dev.theWhiteBread.menus.presets

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.createPlayerHead
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.MenuRegistry
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.menus.createMenuReadyItemFromItemStack
import dev.theWhiteBread.storage_system.controller.StorageController
import dev.theWhiteBread.storage_system.controller.StorageControllerMemberPermission
import dev.theWhiteBread.toSerLocation
import dev.theWhiteBread.toUUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

typealias SCM = StorageControllerMenus


object StorageControllerMenus {

    val controllerSettingsMenu: (Player, StorageController) -> Menu = { player, controller -> run {
        Menu(player, "Controller Settings", traversal = false, rows = 2).apply {
            val items = mutableListOf<MenuItem>()
            items.add(
                MenuItem(
                    createMenuReadyItem(
                        "<#DE1F59>Global Access",
                        listOf(
                            "<gray>Enable global access &",
                            "<gray>Select the permission level for everyone on the server."
                        ),
                        Material.COMMAND_BLOCK
                    )
                ) { _ -> SCM.globalAccessPermissionsMenu(player, controller).open() }
            )

            items.add(
                MenuItem(createMenuReadyItem(
                    "<#3803A1>Member Access",
                    listOf(
                        "<gray>Modify players permissions to your storage manager"
                    ),
                    Material.COMPARATOR
                )) {_ -> SCM.membersPermissionMenu(player, controller).open()}
            )

            setItems(items)
        }
    }}

    val globalAccessPermissionsMenu: (Player, StorageController) -> Menu = { player, controller -> run {
        Menu(player, "Global Access Permissions", rows = 2, traversal = false).apply {
            val items = mutableListOf<MenuItem>()
            setUtility(0, MenuItem(createMenuReadyItem(
                "<bold>Enable Public Access",
                listOf(),
                Material.LODESTONE,
                enchantGlint = controller.data.members.publicAccess.first
            )) { run {
                controller.data.members.publicAccess = Pair(!controller.data.members.publicAccess.first, controller.data.members.publicAccess.second)
                controller.updateViewState()
                controller.save()
                controllerSettingsMenu(player, controller).open()
            }})

            StorageControllerMemberPermission.entries.forEach {
                items.add(MenuItem(createMenuReadyItem(
                    "<bold>${it.name}",
                    listOf("<gray>${it.description}"),
                    Material.ANVIL,
                    enchantGlint = controller.data.members.publicAccess.second == it
                )) { _ -> run {
                    controller.data.members.publicAccess = Pair(controller.data.members.publicAccess.first, it)
                    controller.updateViewState()
                    controller.save()
                    controllerSettingsMenu(player, controller).open()
                } })
            }

            setItems(items)
        }
    }}

    val memberPermissionMenu: (Player, StorageController, OfflinePlayer) -> Menu = {player, controller, member -> run {
        Menu(player, "${member.name} Access Permission", rows = 2, traversal = false).apply {
            val items = mutableListOf<MenuItem>()
            StorageControllerMemberPermission.entries.forEach {
                items.add(MenuItem(createMenuReadyItem(
                    "<bold>${it.name}",
                    listOf("<gray>${it.description}"),
                    Material.ANVIL,
                    enchantGlint = controller.data.members.members[member.uniqueId.toString()] == it
                )) { event -> run {
                    if (event.isRightClick) {
                        controller.data.members.members.remove(member.uniqueId.toString())

                        member.player?.persistentDataContainer?.set(
                            Keys.itemStorageControllerManagerOf,
                            PDC.playerManagerOfStorageControllers,
                            member.player?.persistentDataContainer?.getOrDefault(
                                Keys.itemStorageControllerManagerOf,
                                PDC.playerManagerOfStorageControllers,
                                emptyList())!!
                                .toMutableList().apply {
                                    if (isNotEmpty()) remove(controller.block.location.toSerLocation())
                                }.toList()
                        )

                        controller.updateViewState()
                        controller.save()
                        membersPermissionMenu(player, controller).open()
                    } else {
                        controller.data.members.members[member.uniqueId.toString()] = it

                        member.player?.persistentDataContainer?.set(
                            Keys.itemStorageControllerManagerOf,
                            PDC.playerManagerOfStorageControllers,
                            member.player?.persistentDataContainer?.getOrDefault(
                                Keys.itemStorageControllerManagerOf,
                                PDC.playerManagerOfStorageControllers,
                                listOf(controller.block.location.toSerLocation()))!!
                                .toMutableList().apply {
                                    add(controller.block.location.toSerLocation())
                                }.toList()
                        )

                        controller.updateViewState()
                        controller.save()
                        membersPermissionMenu(player, controller).open()
                    }
                } })
            }

            setItems(items)
        }
    }}

    val membersPermissionMenu: (Player, StorageController) -> Menu = {player, controller -> run {
        Menu(player, "Member Access Permissions").apply {
            val items = mutableListOf<MenuItem>()

            setUtility(0, MenuItem(
                createMenuReadyItem(
                    "<green>Add Members",
                    listOf("<gray>Add list of members to give access"),
                    Material.CHEST
                )
            ){_ -> run {
                MenuRegistry.playerSelectionMenu(player) { event ->
                    val head = event.currentItem ?: return@playerSelectionMenu
                    val headMeta = head.itemMeta as SkullMeta
                    val memberID = headMeta.owningPlayer?.uniqueId ?: return@playerSelectionMenu
                    controller.data.members.members[memberID.toString()] = StorageControllerMemberPermission.VIEWER
                    controller.save()
                    controller.updateViewState()
                    memberPermissionMenu(player, controller, Bukkit.getOfflinePlayer(memberID)).open()
                }.open()
            }})

            controller.data.members.members.mapKeys { it.key.toUUID() }.forEach {
                items.add(MenuItem(createMenuReadyItemFromItemStack(createPlayerHead(Bukkit.getOfflinePlayer(it.key)), lore = listOf("<gray>Permission: <gold>${it.value}"))) { event -> run {
                    if (event.isRightClick) {
                        controller.data.members.members.remove(it.key.toString())
                        controller.save()
                        controller.updateViewState()
                        membersPermissionMenu(player, controller).open()
                    } else memberPermissionMenu(player, controller, Bukkit.getOfflinePlayer(it.key)).open()
                }})
            }

            setItems(items)
        }
    }}

}