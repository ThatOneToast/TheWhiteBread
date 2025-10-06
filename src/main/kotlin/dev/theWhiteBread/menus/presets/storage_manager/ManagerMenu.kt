package dev.theWhiteBread.menus.presets.storage_manager

import dev.theWhiteBread.PDC
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.serializables.LocationSerializer
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.storage_manager.container.StorageContainer
import dev.theWhiteBread.storage_manager.manager.StorageManagerBlock
import dev.theWhiteBread.toSerLocation
import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class ManagerMenu(
    val viewer: Player,
    val manager: StorageManagerBlock
) {

    val view: Menu = Menu(
        player = viewer,
        title = "Storage Manager",
        traversal = true,
        displayFilter = true,
        displayUtilities = true
    ).apply {
        setUtility(0, MenuItem(createMenuReadyItem(
            "<bold>Settings",
            emptyList(),
            Material.DISPENSER,
            false
        )) { event -> run {
            ManagerSettingsMenu(viewer, manager).view.open()
        } }
        )
    }

    init {
        updateViewState()
    }

    fun updateViewState() {
        val menuItems = mutableSetOf<MenuItem>()
        manager.containers.forEach { container ->
            menuItems.add(MenuItem(
                createMenuReadyItem(
                    "<gold>Implement Custom Naming",
                    listOf("<gold>Implement custom lore, with fixed footnotes like location"),
                    Material.BARREL,
                ).apply {
                    val meta = itemMeta
                    PDC.setValueOf<SerializableLocation>(meta.persistentDataContainer, "container_location_item_tag", container.location.toSerLocation())
                    itemMeta = meta
                }
            ) {event -> run {
                if (event.isLeftClick) {
                    val item = event.currentItem ?: return@run
                    val location = PDC.valueOf<SerializableLocation>(item.itemMeta.persistentDataContainer, "container_location_item_tag") ?: return@run
                    val inventory = containerInventories[location.fromSerLocation()] ?: return@run
                    event.whoClicked.openInventory(inventory)
                }
            }})
        }
        view.setItems(menuItems.toList())
    }

    val containerInventories: HashMap<Location, Inventory> = run {
        val map = hashMapOf<Location, Inventory>()
        manager.containers.forEach {
            map[it.location] = it.inventory
        }
        map
    }

}