package dev.theWhiteBread.menus.presets

import dev.theWhiteBread.menus.ClickAction
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.message
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

object ColorMenu {
    /**
     * Open the color selection menu for player.
     * onSelect is invoked with the chosen DyeColor.
     */
    fun open(player: Player, onSelect: (DyeColor) -> Unit = {}) {
        val menu = Menu(player, "Select Color", rows = 3, displayFilter = false, traversal = false)

        val items = DyeColor.entries.map { color ->
            val matName = "${color.name}_DYE"
            val material = try {
                Material.valueOf(matName)
            } catch (ex: IllegalArgumentException) {
                Material.INK_SAC
            }

            val displayName = color.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            val stack = createMenuReadyItem("<aqua>$displayName", emptyList(), material)

            // named handler so we can `return@handler` if needed
            val handler: ClickAction = handler@ { ev: InventoryClickEvent ->
                ev.isCancelled = true
                val who = ev.whoClicked
                if (who !is Player) return@handler
                who.closeInventory()
                who.message("Selected color: $displayName")
                onSelect(color)
            }

            MenuItem(stack, handler)
        }

        menu.setItems(items)
        menu.open(0)
    }
}