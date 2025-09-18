package dev.theWhiteBread.menus

import dev.theWhiteBread.message
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import kotlin.math.min


@Suppress("UNUSED")
class Menu(
    val player: Player,
    private val title: String,
    private val rows: Int = 6,
    private val displayFilter: Boolean = false,
    private val traversal: Boolean = true,
    private var isSubsViewOnly: Boolean = false,
    private var displayUtilities: Boolean = true,
) {

    init {
        require(rows in 1..6) { "rows must be between 2 and 6" }
    }

    val blockClicks = mutableSetOf<ClickType>()

    private val size = rows * 9
    private val contentSize = size - 9

    private val inventories = mutableMapOf<Int, Inventory>()
    private val handlers = mutableMapOf<Inventory, MutableMap<Int, ClickAction>>()
    private var pages = 0
    private val allItems = mutableListOf<MenuItem>()
    private var activeFilter = Filter.ALL
    private val utilities: Array<MenuItem?> = arrayOfNulls(9)

    fun setItems(list: List<MenuItem>) {
        allItems.clear()
        allItems.addAll(list)
        buildPages()
    }

    fun setItem(index: Int, element: MenuItem) {
        allItems[index] = element
        buildPages()
    }

    fun setFilter(filter: Filter) {
        activeFilter = filter
        buildPages()
    }

    fun cycleFilter() {
        val values = Filter.entries
        val next = (values.indexOf(activeFilter) + 1) % values.size
        activeFilter = values[next]
        buildPages()
        open(0)
    }

    fun setUtility(relSlot: Int, item: MenuItem?) {
        require(relSlot in 0..8) { "relSlot must be in 0..8 (bottom row slots)" }
        utilities[relSlot] = item
        buildPages()
    }

    fun setUtilities(map: Map<Int, MenuItem>) {
        utilities.fill(null)
        for ((k, v) in map) {
            require(k in 0..8) { "utility slot keys must be 0..8" }
            utilities[k] = v
        }
        buildPages()
    }

    fun clearUtilities() {
        utilities.fill(null)
        buildPages()
    }

    private fun buildPages() {
        inventories.clear()
        handlers.clear()

        val displayItems: List<MenuItem> = when (activeFilter) {
            Filter.ALL -> {
                allItems
            }
            Filter.BLOCKS -> {
                allItems.filter { it.stack.type.isBlock }.toMutableList()
            }
            else -> {
                allItems.filter { activeFilter.predicate(it.stack) }
            }
        }

        pages = when {
            contentSize <= 0 -> 1
            displayItems.isEmpty() -> 1
            else -> (displayItems.size + contentSize - 1) / contentSize
        }

        for (page in 0 until pages) {
            val titleLine = "$title (${page + 1}/$pages) [${activeFilter.displayName}]"
            val inv = Bukkit.createInventory(player, size, Component.text(titleLine))
            val pageHandlers = mutableMapOf<Int, ClickAction>()

            val start = page * contentSize
            val end = min(displayItems.size, start + contentSize)
            for ((i, idx) in (start until end).withIndex()) {
                val slot = i
                val menuItem = displayItems[idx]
                inv.setItem(slot, menuItem.stack)
                pageHandlers[slot] = { ev ->
                    ev.isCancelled = true
                    if (!isSubsViewOnly) menuItem.onClick(ev) else player.message("<red>This is a view only menu.")
                }
            }

            val navStart = size - 9

            for (relSlot in 0 until BOTTOM_ROW_SLOTS) {
                val absSlot = navStart + relSlot
                val util = utilities[relSlot]

                if (displayUtilities) {
                    if (util != null) {
                        inv.setItem(absSlot, util.stack)
                        pageHandlers[absSlot] = { ev ->
                            ev.isCancelled = true
                            util.onClick(ev)
                        }
                        continue
                    }
                }

                when (relSlot) {
                    REL_PREV_SLOT -> if (traversal) {
                        inv.setItem(
                            absSlot,
                            createMenuReadyItem("<green>Previous", emptyList(), Material.ARROW)
                        )
                        pageHandlers[absSlot] = { ev ->
                            ev.isCancelled = true
                            if (page > 0) open(page - 1)
                        }
                    }

                    REL_NEXT_SLOT -> if (traversal) {
                        inv.setItem(
                            absSlot,
                            createMenuReadyItem("<green>Next", emptyList(), Material.ARROW)
                        )
                        pageHandlers[absSlot] = { ev ->
                            ev.isCancelled = true
                            if (page < pages - 1) open(page + 1)
                        }
                    }

                    REL_CLOSE_SLOT -> if (traversal) {
                        inv.setItem(
                            absSlot,
                            createMenuReadyItem("<red>Close", emptyList(), Material.BARRIER)
                        )
                        pageHandlers[absSlot] = { ev ->
                            ev.isCancelled = true
                            player.closeInventory()
                        }
                    }

                    REL_FILTER_SLOT -> if (displayFilter) {
                        inv.setItem(
                            absSlot,
                            createMenuReadyItem(
                                "<yellow>${activeFilter.displayName}",
                                emptyList(),
                                activeFilter.icon
                            )
                        )
                        pageHandlers[absSlot] = { ev ->
                            ev.isCancelled = true
                            cycleFilter()
                        }
                    }

                    else -> {
                        // leave empty
                    }
                }
            }

            inventories[page] = inv
            handlers[inv] = pageHandlers
        }
    }

    fun open(page: Int = 0) {
        if (pages == 0) return
        val p = page.coerceIn(0, pages - 1)
        val inv = inventories[p] ?: return
        player.openInventory(inv)
        MenuRegistry.openMenus[player.uniqueId] = this
    }

    fun openViewOnly(page: Int = 0) {
        isSubsViewOnly = true
        buildPages()
        open(page)
    }

    fun handleClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val map = handlers[top] ?: return
        if (event.rawSlot >= top.size) return // clicked player inventory
        if (blockClicks.contains(event.click)) return
        val action = map[event.rawSlot] ?: return
        action(event)
    }

    /**
     * Returns a copy of this menu
     * Replace any argument for the new copy.
     */
    fun copy(
        player: Player = this.player,
        title: String = this.title,
        rows: Int = this.rows,
        displayFilter: Boolean = this.displayFilter,
        traversal: Boolean = this.traversal,
        isSubsViewOnly: Boolean = this.isSubsViewOnly,
        displayUtilities: Boolean = this.displayUtilities
    ): Menu {
        val copy = Menu(player, title, rows, displayFilter, traversal, isSubsViewOnly, displayUtilities)

        copy.allItems.clear()
        copy.allItems.addAll(this.allItems.map { MenuItem(it.stack.clone(), it.onClick) })

        copy.utilities.fill(null)
        for (i in this.utilities.indices) {
            val util = this.utilities[i]
            if (util != null) {
                copy.utilities[i] = MenuItem(util.stack.clone(), util.onClick)
            }
        }

        copy.activeFilter = this.activeFilter
        copy.buildPages()
        return copy
    }


    fun handlesInventory(inv: Inventory) = handlers.containsKey(inv)

    private companion object {
        private const val BOTTOM_ROW_SLOTS = 9
        private const val REL_PREV_SLOT = 3
        private const val REL_FILTER_SLOT = 4
        private const val REL_NEXT_SLOT = 5
        private const val REL_CLOSE_SLOT = 8
    }

}