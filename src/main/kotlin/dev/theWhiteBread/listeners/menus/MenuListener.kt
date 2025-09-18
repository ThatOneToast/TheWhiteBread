package dev.theWhiteBread.listeners.menus

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.menus.MenuRegistry
import dev.theWhiteBread.message
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

object MenuListener : BreadListener {

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val menu = MenuRegistry.openMenus[player.uniqueId] ?: return

        if (menu.handlesInventory(e.view.topInventory)) {
            e.isCancelled = true
            menu.handleClick(e)
        } else if (MenuRegistry.openViewOnly.contains(player.uniqueId)) {
            val inv = MenuRegistry.openViewOnly[player.uniqueId] ?: return
            if (inv != e.view.topInventory) return

            if (e.rawSlot < e.view.topInventory.size) {
                player.message("<red>This is a <bold>View Only</bold> menu.")
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(e: InventoryDragEvent) {
        val player = e.whoClicked as? Player ?: return
        val inv = MenuRegistry.openViewOnly[player.uniqueId] ?: return
        if (inv != e.view.topInventory) return

        if (e.rawSlots.any { it < e.view.topInventory.size }) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(e: InventoryCloseEvent) {
        val player = e.player as? Player ?: return
        val menu = MenuRegistry.openMenus[player.uniqueId] ?: return
        if (menu.handlesInventory(e.inventory)) {
            MenuRegistry.openMenus.remove(player.uniqueId)
        } else if (MenuRegistry.openViewOnly.contains(player.uniqueId)) {
            val inv = MenuRegistry.openViewOnly[player.uniqueId] ?: return
            if (inv == e.inventory) {
                MenuRegistry.openViewOnly.remove(player.uniqueId)
            }
        }
    }

}