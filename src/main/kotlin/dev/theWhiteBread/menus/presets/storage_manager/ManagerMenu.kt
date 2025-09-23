package dev.theWhiteBread.menus.presets.storage_manager

import dev.theWhiteBread.menus.Menu
import org.bukkit.entity.Player

class ManagerMenu(
    val viewer: Player
) {

    val view: Menu = Menu(
        player = viewer,
        title = "Storage Manager",
        traversal = true,
        displayFilter = true,
        displayUtilities = true
    ).apply {

    }


}