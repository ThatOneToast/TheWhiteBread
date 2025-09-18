package dev.theWhiteBread.menus

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.createPlayerHead
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object MenuRegistry {
    val openMenus = mutableMapOf<UUID, Menu>()
    val openViewOnly = mutableMapOf<UUID, Inventory>()

    val playerSelectionMenu: (Player, ClickAction) -> Menu = { player, playerClickHeadEvent -> run {
        val players = Bukkit.getOfflinePlayers()
        Menu(player, "Player Selection").apply {
            val items = mutableListOf<MenuItem>()
            players.forEach {
                items.add(MenuItem(createPlayerHead(it), playerClickHeadEvent))
            }
            setItems(items)
        }
    }}



}