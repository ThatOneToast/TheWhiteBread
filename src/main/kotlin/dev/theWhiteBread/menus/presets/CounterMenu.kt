package dev.theWhiteBread.menus.presets

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import java.util.concurrent.atomic.AtomicInteger

class CounterMenu(
    private val player: Player,
    initial: Int = 0,
    private val minValue: Int = Int.MIN_VALUE,
    private val maxValue: Int = Int.MAX_VALUE,
    private val onClose: (Int) -> Unit = {},
) {
    private val counter = AtomicInteger(initial)

    fun getValue(): Int = counter.get()

    // rows = 3 -> size = 27 -> contentSize = 18
    private val paginated = Menu(player, "Counter",
        rows = 3, displayFilter = false,
        traversal = false
    )

    // Content indices (0..contentSize-1). Tweak positions if needed.
    private val idxMinus100 = 0
    private val idxMinus10 = 1
    private val idxMinus5 = 2
    private val idxMinus1 = 3
    private val idxDisplay = 4
    private val idxPlus1 = 5
    private val idxPlus5 = 6
    private val idxPlus10 = 7
    private val idxPlus100 = 8

    fun open() {
        TheWhiteBread.instance.server.pluginManager.registerEvents(closeListener, TheWhiteBread.instance)
        paginated.setItems(buildItems())
        paginated.open(0)
    }

    val closeListener = object : Listener {
        @EventHandler
        fun onInventoryClose(ev: InventoryCloseEvent) {
            // ensure it's the same player
            if (ev.player.uniqueId != player.uniqueId) return

            // ensure the closed inventory belongs to this paginated menu
            val top = ev.view.topInventory
            if (!paginated.handlesInventory(top)) return

            // call the provided callback with the final value
            onClose(counter.get())


            // unregister this listener so it only runs once
            HandlerList.unregisterAll(this)
        }
    }

    private fun buildItems(): List<MenuItem> {
        val contentSize = (3 * 9) - 9 // rows * 9 - 9 = 18
        val items = MutableList<MenuItem>(contentSize) {
            // filler pane no-op
            MenuItem(
                createMenuReadyItem("<gray> ", emptyList(), Material.GRAY_STAINED_GLASS_PANE)
            ) { ev -> ev.isCancelled = true }
        }

        fun applyChange(delta: Int) {
            val next = (counter.get() + delta).coerceIn(minValue, maxValue)
            counter.set(next)
            // Rebuild items and update menu (re-registers handlers)
            paginated.setItems(buildItems())
            paginated.open(0)
        }

        items[idxMinus100] = MenuItem(
            createMenuReadyItem("<red>-100", listOf("<white>Decrease by 100"), Material.RED_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(-100)
        }

        items[idxMinus10] = MenuItem(
            createMenuReadyItem("<red>-10", listOf("<white>Decrease by 10"), Material.RED_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(-10)
        }

        items[idxMinus5] = MenuItem(
            createMenuReadyItem("<red>-5", listOf("<white>Decrease by 5"), Material.RED_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(-5)
        }

        items[idxMinus1] = MenuItem(
            createMenuReadyItem("<red>-1", listOf("<white>Decrease by 1"), Material.RED_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(-1)
        }

        // Display slot (center)
        items[idxDisplay] = MenuItem(
            createMenuReadyItem(
                "<gold>Value: <white>${counter.get()}",
                listOf("<gray>Click +/- to change"),
                Material.PAPER
            )
        ) { ev -> ev.isCancelled = true } // no-op on click


        items[idxPlus1] = MenuItem(
            createMenuReadyItem("<green>+1", listOf("<white>Increase by 1"), Material.GREEN_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(+1)
        }

        items[idxPlus5] = MenuItem(
            createMenuReadyItem("<green>+5", listOf("<white>Increase by 5"), Material.GREEN_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(+5)
        }

        items[idxPlus10] = MenuItem(
            createMenuReadyItem("<green>+10", listOf("<white>Increase by 10"), Material.GREEN_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(+10)
        }

        items[idxPlus100] = MenuItem(
            createMenuReadyItem("<green>+100", listOf("<white>Increase by 100"), Material.GREEN_WOOL)
        ) { ev ->
            ev.isCancelled = true
            applyChange(+100)
        }

        // Optionally put Close button in content too (you already have nav close)
        // items[contentSize - 1] = MenuItem(createMenuReadyItem("<red>Close", emptyList(), Material.BARRIER)) { ev ->
        //   ev.isCancelled = true
        //   player.closeInventory()
        // }

        return items
    }
}