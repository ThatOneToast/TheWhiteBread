package dev.theWhiteBread.menus.presets.storage_manager

import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import org.bukkit.entity.Player

class ManagerSettingsMenu(
    val viewer: Player,
    val manager: StorageManagerBlock

) {

    val view: Menu = Menu(
        player = viewer,
        title = "Storage Manager Settings",
        traversal = false,
        displayFilter = false,
        displayUtilities = true
    ).apply {

    }

}
