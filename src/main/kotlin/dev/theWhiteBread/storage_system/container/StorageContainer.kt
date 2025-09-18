package dev.theWhiteBread.storage_system.container

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.RateLimiter
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.listeners.input.ChatInput
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.MenuRegistry
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.menus.presets.CounterMenu
import dev.theWhiteBread.message
import dev.theWhiteBread.miniMessage
import dev.theWhiteBread.replaceVars
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.toMenuItemList
import dev.theWhiteBread.toSerLocation
import dev.theWhiteBread.updateStorageContainer
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.toString

typealias StorageContainer = StorageContainerVersions.Version2

fun StorageContainer.getBlock(): Block = this.location.toBlock()


object StorageContainerVersions {


    @Serializable
    data class Version1(
        val location: SerializableLocation,
        val customization: ContainerCustomization,
        val parentControllerLocation: SerializableLocation
    )

    @Serializable
    data class Version2(
        val location: SerializableLocation,
        val customization: ContainerCustomization,
        val parentControllerLocation: SerializableLocation,
        val containerType: ContainerType = ContainerType.matchBlockToType(location.toBlock())!!,
        val id: String = UUID.randomUUID().toString(),
        val configVersion: Int = 2
    )


}

fun StorageContainer.configMenu(p: Player): Menu {
    val menu = Menu(p, "", rows = 2, traversal = false)

    val containerBlock = this.getBlock()
    val containerState = containerBlock.state as Container

    val controller = ControllerRegistry.getController(containerBlock.chunk) ?: throw IllegalArgumentException("${containerBlock.chunk} is not a valid controller")

    val containerData = containerState.persistentDataContainer.get(Keys.itemStorageContainer, PDC.chunkStorageContainer) ?: StorageContainer(
        containerState.location.toSerLocation(),
        ContainerCustomization(),
        controller.block.location.toSerLocation()
    )


    val nameTag = MenuItem(createMenuReadyItem(
        ("<gold>Change Name"),
        listOf(
            ("<gray>Change the display name of this storage container.")
        ),
        Material.NAME_TAG
    ))
    { event -> run {
        val player = event.whoClicked as Player
        player.message(("<white>Please type the new name of the <green>container"))
        val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!
        player.closeInventory()

        ChatInput.registerNewInputListener(player.uniqueId) { player, message ->
            run {

                object : BukkitRunnable() {
                    override fun run() {
                        player.message(("<green>Name change successful!"))
                        containerData.customization.displayName = miniMessage.serialize(miniMessage.deserialize(message))
                        containerState.updateStorageContainer(containerData)
                        inv.open()
                    }

                }.runTask(TheWhiteBread.instance)


                ChatInput.finishInputListening(player.uniqueId)
            }
        }

    }}


    val description = MenuItem(createMenuReadyItem(
        ("<gold>Set Description"),
        listOf(
            ("<gray>Give a neat description about this container")
        ),
        Material.ANVIL
    ))
    { event -> run {
        val player = event.whoClicked as Player
        val descriptionLines = mutableListOf<String>()

        val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!
        player.closeInventory()
        player.message("<gray>Start sending your descriptions lines. Type: \"EXIT()\" to finish early. 6 lines total.")

        ChatInput.registerNewInputListener(player.uniqueId) { player: Player, msg: String -> run {
            var finished = false
            if (msg == "EXIT()") {
                finished = true
            } else {
                if (descriptionLines.size + 1 >= 6) {
                    player.message("<gray> You have reached your limit. Your changes will be saved.")
                    finished = true
                } else {
                    val msg = msg.replaceVars(
                        mapOf(
                            "location" to "X: ${containerData.location.x} Y: ${containerData.location.y}, Z: ${containerData.location.z}",
                            "type" to containerData.getBlock().type.toString(),
                            "size" to containerState.inventory.contents.filter{it?.type != Material.AIR}.size.toString(),
                            "maxSize" to containerState.inventory.size.toString()
                        )
                    )
                    descriptionLines.add(msg)
                    player.persistentDataContainer.set(Keys.getPlayerInput, PersistentDataType.BOOLEAN, true)
                    player.message("<gray>To finish appending your description type: <green>\"EXIT()\"</green>")
                    player.message("<gray>Current length: ${descriptionLines.size}/6")
                }
            }

            if (finished) {
                object : BukkitRunnable() {
                    override fun run() {
                        ChatInput.finishInputListening(player.uniqueId)
                        containerData.customization.description = descriptionLines
                        containerState.updateStorageContainer(containerData)
                        inv.open()
                    }

                }.runTask(TheWhiteBread.instance)
            }

        }}

    } }

    val displayItem = MenuItem(createMenuReadyItem(
        ("<gold>Icon"),
        listOf(
            ("<gray>Select an icon to use for your container")
        ),
        Material.PAINTING
    ))
    { event -> run {
        val player = event.whoClicked as Player
        val allMaterials = ItemRegistry.items
        val menuItems = mutableListOf<MenuItem>()
        val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!
        allMaterials.forEach { stack ->
            menuItems.add(MenuItem(stack) {
                containerData.customization.displayItem = stack.type
                containerState.updateStorageContainer(containerData)
                inv.open()
            })
        }

        val materialSelectionMenu = Menu(player, "Material Selection", displayFilter = true).apply { setItems(menuItems) }
        materialSelectionMenu.open()

    }}

    val exportConfig = MenuItem(createMenuReadyItem(
        "<red>Export<gold> Configuration",
        listOf(
            "<gray>Exporting configuration"
        ),
        Material.HOPPER
    )) { _ -> run {
        when (containerData.containerType) {
            ContainerType.INVENTORY -> {
                 Menu(p, "Exporting Inventory Configuration", rows = 2, traversal = false).apply {
                    setItems(listOf(
                        MenuItem(createMenuReadyItem(
                            ("<gold>Toggle Exporting"),
                            listOf(
                                ("<gray>Export items into another storage container"),
                            ),
                            Material.HOPPER,
                            containerData.customization.exporter.exporting
                        ))
                        { run {

                            containerData.customization.exporter.exporting = !containerData.customization.exporter.exporting
                            containerState.updateStorageContainer(containerData)
                            menu.open()

                        }},
                        MenuItem(createMenuReadyItem(
                            "<red>Export<gold> Whitelist",
                            listOf("<gray>Select the items that are available for exporting, along with reserve amounts"),
                            Material.BARRIER
                        ))
                        { event -> run {
                            val player = event.whoClicked as Player
                            val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!
                            val menuMaterialSelection = ItemRegistry.menuItems.toList().apply{
                                forEach { menuItem ->
                                    menuItem.onClick = {run {
                                        val counterMenu = CounterMenu(
                                            player,
                                            0,
                                            1,
                                            1000
                                        )
                                        { value ->
                                            run {
                                                val list =
                                                    containerData.customization.exporter.whiteList.toMutableList()
                                                val prevValue = list.find { it.first == menuItem.stack.type }
                                                if (prevValue != null) list.remove(Pair(menuItem.stack.type, value))
                                                list.add(Pair(menuItem.stack.type, value))
                                                containerData.customization.exporter.whiteList = list

                                                containerState.updateStorageContainer(containerData)
                                                RateLimiter.runTicksLater(1) {
                                                    inv.open()
                                                }
                                            }
                                        }

                                        counterMenu.open()

                                    } }
                                }
                            }

                            val materialSelection = Menu(player, "Material Selection", displayFilter = true).apply {
                                setItems(menuMaterialSelection)
                            }

                            val materialReserve: (material: Material) -> Int = { material ->
                                val list = containerData.customization.exporter.whiteList.toMutableList()
                                val item = list.find { it.first == material }!!
                                item.second
                            }

                            val currentlyWhitelistedItems = (containerData.customization.exporter.whiteList.toMutableList()).map {
                                    counterStack -> MenuItem(
                                createMenuReadyItem(
                                    "<gold><bold>${counterStack.first.toString().lowercase()}</bold></gold>: Reserved: <green>${materialReserve(counterStack.first)}</green>",
                                    listOf(
                                        "<gray>This container will keep at least <gold>${materialReserve(counterStack.first)}</gold> ${counterStack.first.toString().lowercase()} in stock.",
                                        "<gray>This item is now available for <green>exporting</green>/<green>importing</green>. ",
                                        "<red><bold>Right clicking this item will remove it from your whitelist."
                                    ),
                                    counterStack.first,
                                    true
                                )
                            ) { event ->
                                if (event.isLeftClick) {
                                    val counterMenu = CounterMenu(
                                        player,
                                        counterStack.second,
                                        1,
                                        1000
                                    ) { endValue ->
                                        run {
                                            val list = containerData.customization.exporter.whiteList.toMutableList()
                                            val idx = list.find { it.first == counterStack.first }
                                                ?: throw IllegalStateException("Player selected a material within material selection that wasn't reported in their registry.")
                                            list.remove(idx)
                                            list.add(Pair(counterStack.first, endValue))
                                            containerState.updateStorageContainer(containerData)
                                            RateLimiter.runTicksLater(1) {
                                                inv.open()
                                            }

                                        }
                                    }

                                    counterMenu.open()
                                }

                                else if (event.isRightClick) {
                                    val materialClicked = event.currentItem ?: return@MenuItem
                                    val list = containerData.customization.exporter.whiteList.toMutableList()
                                    val candidate = list.find { it.first == materialClicked.type } ?: return@MenuItem
                                    list.remove(candidate)
                                    containerData.customization.exporter.whiteList = list
                                    containerState.updateStorageContainer(containerData)
                                    player.message("<red>Deleted whitelist entry ${materialClicked.type.toString().lowercase()}")
                                    inv.open()
                                }

                            } }

                            val whiteListMenu = Menu(player, "Whitelisting", displayFilter = true).apply {
                                setItems(currentlyWhitelistedItems)
                                setUtility(0, MenuItem(
                                    createMenuReadyItem(
                                        "<green>Add Materials",
                                        listOf("<gray>Add more materials to your whitelist"),
                                        Material.CHEST
                                    )
                                ) {
                                    materialSelection.open()
                                })
                            }

                            whiteListMenu.open()

                        } }
                    ))
                }.open()
            }

            ContainerType.SMELTING -> {}
            ContainerType.BREWING -> {}
        }

    } }

    val importConfig = MenuItem(
        createMenuReadyItem(
            ("<gold><green>Import</green> Configuration"),
            listOf(
                ("<gray>Select exporting containers to import from."),
                ("<gray>Your imports will follow your import white list."),
                "<gray>You can have a container exporting and importing. Their whitelist",
                "<gray>and reserve amounts are separated"
            ),
            Material.CHEST,
            containerData.customization.importer.importFrom.isNotEmpty()
        )
    )
    { event ->
        run {
            val player = event.whoClicked as Player
            val importConfigMenu = Menu(player, "Importing Configuration", 2, traversal = false)
                .apply importConfigMenu@{
                    setItems(
                        listOf(
                        MenuItem(
                            createMenuReadyItem(
                                "<gold>Importing From",
                                listOf("<gray>View and Add containers to import from."),
                                Material.HOPPER
                            )
                        )
                        {
                            run {
                                val currentImportingContainers =
                                    containerData.customization.importer.importFrom.toMutableList()
                                val menuList = currentImportingContainers.toMenuItemList { container, clickEvent ->
                                    run {
                                        if (clickEvent.isRightClick) {
                                            currentImportingContainers.remove(container)
                                            containerData.customization.importer.importFrom = currentImportingContainers
                                            containerState.updateStorageContainer(containerData)
                                            this@importConfigMenu.open()
                                        }
                                    }
                                }
                                val importFromContainersMenu = Menu(
                                    player,
                                    "Importing From ${menuList.size} Container(s)",
                                    displayFilter = true
                                ).apply importFromContainersMenu@{
                                    setItems(menuList)
                                    setUtility(
                                        1,
                                        MenuItem(
                                            createMenuReadyItem(
                                                "<green>Add containers",
                                                listOf("<gray>Opens a menu of all available exporting containers to choose from"),
                                                Material.CHEST
                                            )
                                        )
                                        {
                                            run {
                                                val exportingContainers = controller.getContainerData()
                                                    .filter { it.customization.exporter.exporting && it.location != containerData.location }
                                                    .toMenuItemList { container, _ ->
                                                        currentImportingContainers.add(container)
                                                        containerData.customization.importer.importFrom =
                                                            currentImportingContainers
                                                        containerState.updateStorageContainer(containerData)
                                                        this@importConfigMenu.open()
                                                    }

                                                val exportingContainersMenu = Menu(
                                                    player,
                                                    "Exporting Containers",
                                                    displayFilter = true
                                                ).apply {
                                                    setItems(exportingContainers)
                                                }

                                                exportingContainersMenu.open()

                                            }
                                        })
                                }
                                importFromContainersMenu.open()

                            }
                        },

                        MenuItem(
                            createMenuReadyItem(
                                ("<green>Import<gold> Whitelist"),
                                listOf(
                                    ("<gray>Select the items that will be imported from containers.")
                                ),
                                Material.BARRIER
                            )
                        )
                        { event ->
                            run {
                                val player = event.whoClicked as Player
                                val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!

                                val menuMaterialSelection = ItemRegistry.menuItems.toList()
                                    .apply {
                                        forEach { menuItem ->
                                            menuItem.onClick = {
                                                run {
                                                    val counterMenu = CounterMenu(
                                                        player,
                                                        0,
                                                        1,
                                                        1000
                                                    )
                                                    { value ->
                                                        run {
                                                            val list =
                                                                containerData.customization.importer.whiteList.toMutableList()
                                                            val prevValue =
                                                                list.find { it.first == menuItem.stack.type }
                                                            if (prevValue != null) list.remove(
                                                                Pair(
                                                                    menuItem.stack.type,
                                                                    value
                                                                )
                                                            )
                                                            list.add(Pair(menuItem.stack.type, value))
                                                            containerData.customization.importer.whiteList = list

                                                            containerState.updateStorageContainer(containerData)
                                                            player.message("Updated your whitelist")
                                                            RateLimiter.runTicksLater(1) {
                                                                inv.open()
                                                            }
                                                        }
                                                    }

                                                    counterMenu.open()

                                                }
                                            }
                                        }
                                    }

                                val materialSelection =
                                    Menu(player, "Material Selection", displayFilter = true)
                                        .apply {
                                            setItems(menuMaterialSelection)
                                        }

                                val materialReserve: (material: Material) -> Int = { material ->
                                    val list = containerData.customization.importer.whiteList.toMutableList()
                                    val item = list.find { it.first == material }!!
                                    item.second
                                }

                                val currentlyWhitelistedItems =
                                    (containerData.customization.importer.whiteList.toMutableList()).map { counterStack ->
                                        MenuItem(
                                            createMenuReadyItem(
                                                "<gold><bold>${
                                                    counterStack.first.toString().lowercase()
                                                }</bold></gold>: Reserved: <green>${materialReserve(counterStack.first)}</green>",
                                                listOf(
                                                    "<gray>This container will import no more than <gold>${
                                                        materialReserve(
                                                            counterStack.first
                                                        )
                                                    }</gold> ${counterStack.first.toString().lowercase()}.",
                                                    "<gray>This item is now available for <green>exporting</green>/<green>importing</green>. ",
                                                    "<red><bold>Right clicking this item will remove it from your whitelist."
                                                ),
                                                counterStack.first,
                                                true
                                            )
                                        ) { event ->
                                            val inv = MenuRegistry.openMenus[event.whoClicked.uniqueId]!!
                                            if (event.isLeftClick) {
                                                val counterMenu = CounterMenu(
                                                    player,
                                                    counterStack.second,
                                                    1,
                                                    1000
                                                ) { endValue ->
                                                    run {
                                                        val list =
                                                            containerData.customization.importer.whiteList.toMutableList()
                                                        val idx =
                                                            list.find { it.first == counterStack.first }
                                                                ?: throw IllegalStateException(
                                                                    "Player selected a material within material selection that wasn't reported in their registry."
                                                                )
                                                        list.remove(idx)
                                                        list.add(Pair(counterStack.first, endValue))
                                                        containerData.customization.importer.whiteList = list
                                                        containerState.updateStorageContainer(containerData)
                                                        RateLimiter.runTicksLater(1) {
                                                            inv.open()
                                                        }

                                                    }
                                                }

                                                counterMenu.open()
                                            } else if (event.isRightClick) {
                                                val materialClicked = event.currentItem ?: return@MenuItem
                                                val list =
                                                    containerData.customization.importer.whiteList.toMutableList()
                                                val candidate = list.find { it.first == materialClicked.type }
                                                    ?: return@MenuItem
                                                list.remove(candidate)
                                                containerData.customization.importer.whiteList = list
                                                containerState.updateStorageContainer(containerData)
                                                player.message(
                                                    "<red>Deleted whitelist entry ${
                                                        materialClicked.type.toString().lowercase()
                                                    }"
                                                )
                                                inv.open()
                                            }

                                        }
                                    }


                                val whiteListMenu = Menu(player, "Whitelisting", displayFilter = true).apply {
                                    setItems(currentlyWhitelistedItems)
                                    setUtility(
                                        0, MenuItem(
                                            createMenuReadyItem(
                                                "<green>Add Materials",
                                                listOf("<gray>Add more materials to your whitelist"),
                                                Material.CHEST
                                            )
                                        ) {
                                            materialSelection.open()
                                        })
                                }

                                whiteListMenu.open()
                            }
                        }
                    ))
                }

            when (containerData.containerType) {
                ContainerType.BREWING -> {

                }

                ContainerType.INVENTORY -> importConfigMenu.open()
                ContainerType.SMELTING -> {
                    Menu(
                        player,
                        "Furnace Import Configuration",
                        rows = 2,
                        traversal = false,
                        displayUtilities = false
                    ).apply {
                        setItems(
                            listOf(
                            MenuItem(
                                createMenuReadyItem(
                                    "<gray><bold>Fuels",
                                    listOf("<gray>Select allowable sources of fuel to import."),
                                    Material.CHARCOAL
                                )
                            ) {

                            },

                            MenuItem(
                                createMenuReadyItem(
                                    "<white><bold>Burnables",
                                    listOf("<gray>Select materials to which smelt."),
                                    Material.IRON_INGOT
                                )
                            ) {

                            }


                        ))
                    }.open()
                }
            }

        }
    }


    menu.setItems(listOf(nameTag, description, displayItem, exportConfig, importConfig))

    return menu
}

@Serializable
enum class ContainerType {
    INVENTORY,
    SMELTING,
    BREWING

    ;

    companion object
    {
        @JvmStatic
        fun matchBlockToType(block: Block): ContainerType? {
            return when (block.type) {
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL -> INVENTORY
                Material.FURNACE, Material.SMOKER, Material.BLAST_FURNACE -> SMELTING
                Material.BREWING_STAND -> BREWING
                else -> null
            }
        }
    }
}

