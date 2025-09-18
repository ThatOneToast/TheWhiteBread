package dev.theWhiteBread.storage_system.controller

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.locationToString
import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.menus.presets.SCM
import dev.theWhiteBread.menus.presets.StorageControllerMenus
import dev.theWhiteBread.parseLocation
import dev.theWhiteBread.parsePipedStrings
import dev.theWhiteBread.serializables.SerializableLocation
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.container.StorageContainer
import dev.theWhiteBread.storage_system.container.ContainerCustomization
import dev.theWhiteBread.storage_system.container.ContainerType
import dev.theWhiteBread.storage_system.container.configMenu
import dev.theWhiteBread.toBlocks
import dev.theWhiteBread.toSerLocation
import dev.theWhiteBread.toSerializedLocationMap
import dev.theWhiteBread.toUUID
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.TileState
import org.bukkit.block.data.type.Chest
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.persistence.PersistentDataType
import java.time.Instant
import kotlin.collections.MutableList
import kotlin.collections.forEach
import kotlin.collections.iterator

data class StorageController(
    val owner: String,
    val block: Block,
    val view: Menu,
    val data: StorageControllerData,
)
{

    val storageContainers: MutableMap<Chest.Type, MutableList<Block>> = data.containers.mapValues { it.value.toBlocks().toMutableList() }.toMutableMap()
    var memoryChunkContainers: Pair<Instant, MutableMap<Chest.Type, MutableList<Block>>> = Pair(Instant.now().minusSeconds(60), storageContainers)
    val player = TheWhiteBread.instance.server.getPlayer(owner.toUUID()) ?: onlineManagers(data).first()

    fun isOwnerOnline(): Boolean {
        return owner.toUUID() == player.uniqueId
    }

    init {
        view.apply view@ {
            setUtility(0, MenuItem(
                createMenuReadyItem(
                    "<bold><gray>Controller Settings",
                    listOf("<gray>Manage controller settings."),
                    Material.DISPENSER
                )
            ) { event ->
                SCM.controllerSettingsMenu((event.whoClicked as Player), this@StorageController).open()
            })

            setUtility(7, MenuItem(createMenuReadyItem(
                "<bold><#C40424>Refresh View",
                listOf("<gray>Your containers not displaying? Try refreshing."),
                Material.REPEATING_COMMAND_BLOCK
            )) { event -> run {
                if (memoryChunkContainers.first.isBefore(Instant.now().plusSeconds(60))) {
                    scanChunkReplaceMemoryContainers()
                    updateViewState()
                    this@view.copy(event.whoClicked as Player).open()
                } else updateViewState()
            }})
        }

    }

    fun cleanseChunkContainers() {
        val containers = memoryChunkContainers.second

        val lrSet = hashSetOf<SerializableLocation>().apply {
            containers[Chest.Type.LEFT]?.forEach { add(it.location.toSerLocation()) }
            containers[Chest.Type.RIGHT]?.forEach { add(it.location.toSerLocation()) }
        }

        val singles = containers[Chest.Type.SINGLE]
        if (!singles.isNullOrEmpty()) {
            val toRemove = singles.filter { it.location.toSerLocation() in lrSet }
            if (toRemove.isNotEmpty()) {
                singles.removeAll(toRemove)
                toRemove.forEach { b ->
                    val state = b.state as? TileState
                    state?.let {
                        it.persistentDataContainer.remove(Keys.itemStorageContainer)
                        it.update(true, false)
                    }
                }
            }
        }

        memoryChunkContainers = Pair(Instant.now(), containers)
        data.containers = containers.toSerializedLocationMap()
        updateViewState()
        save()
    }

    fun save() {
        val blockState = block.state as TileState
        data.containers = memoryChunkContainers.second.toSerializedLocationMap()
        blockState.persistentDataContainer.set(Keys.itemStorageController, PDC.storageControllerData, data)
        blockState.update(true, false)
    }

    fun scanChunkReplaceMemoryContainers() {
        val chunk = block.chunk
        val containers = mutableMapOf<Chest.Type, MutableList<Block>>(
            Chest.Type.LEFT to mutableListOf(),
            Chest.Type.RIGHT to mutableListOf(),
            Chest.Type.SINGLE to mutableListOf()
        )
        for (x in 0..15) for (z in 0..15) for (y in chunk.world.minHeight until chunk.world.maxHeight) {
            val block = chunk.getBlock(x, y, z)
            if (block.state is Container) {
                when (block.type) {
                    Material.CHEST, Material.TRAPPED_CHEST -> {
                        val blockChest = block.blockData as Chest
                        when (blockChest.type) {
                            Chest.Type.LEFT -> containers[Chest.Type.LEFT]!!.add(block)
                            Chest.Type.SINGLE -> containers[Chest.Type.SINGLE]!!.add(block)
                            Chest.Type.RIGHT -> containers[Chest.Type.RIGHT]!!.add(block)
                        }
                    }
                    Material.BARREL, Material.FURNACE, Material.SMOKER,
                    Material.BLAST_FURNACE, Material.BREWING_STAND -> containers[Chest.Type.SINGLE]!!.add(block)
                    else -> {}
                }
            }
        }
        memoryChunkContainers = Pair(Instant.now(), containers)
    }

    fun addContainer(location: Location) {
        val world = Bukkit.getWorld(location.world.uid) ?: run {
            TheWhiteBread.pluginLogger.warning("World not loaded for $location")
            return
        }

        val blk = world.getBlockAt(location)

        memoryChunkContainers.second.values.forEach { list ->
            list.removeIf { it.location.toSerLocation() ==
                    blk.location.toSerLocation() }
        }

        val chestData = blk.blockData as? Chest
        val key = chestData?.type ?: Chest.Type.SINGLE
        memoryChunkContainers.second.getOrPut(key) { mutableListOf() }.add(blk)

        save()
    }

    fun getContainerData(): List<StorageContainer> {
        val list = mutableListOf<StorageContainer>()
        val seen = mutableSetOf<String>()

        val snapshot = memoryChunkContainers.second.values.flatten()

        for (containerBlock in snapshot) {
            val state = containerBlock.state as? TileState ?: continue

            val container = state.persistentDataContainer.get(
                Keys.itemStorageContainer,
                PDC.chunkStorageContainer
            ) ?: continue

            if (container.id in seen) continue
            seen.add(container.id)
            list.add(container)
        }

        return list
    }


    fun removeContainer(container: Block) {
        val chestData = container.blockData as? Chest
        if (chestData != null) {
            memoryChunkContainers.second[chestData.type]?.remove(container)
        } else {
            memoryChunkContainers.second[Chest.Type.SINGLE]?.remove(container)
        }
        save()
    }

    fun updateViewState(additionalItems: List<MenuItem>? = null) {
        val items = mutableListOf<MenuItem>()
        val snapshot = memoryChunkContainers.second.mapValues { it.value.toList() }
        val seenIds = mutableSetOf<String>()

        for ((itsType, blockList) in snapshot) {
            if (itsType == Chest.Type.RIGHT) continue
            val toRemove = mutableListOf<Block>()

            for (b in blockList) {
                if (b.type == Material.AIR) {
                    TheWhiteBread.pluginLogger.warning("A storage container was AIR! Auto-Removing")
                    toRemove.add(b)
                    continue
                }

                val location = b.location
                val state = b.state as? TileState ?: continue

                val containerData: StorageContainer = state.persistentDataContainer.getOrDefault(
                    Keys.itemStorageContainer,
                    PDC.chunkStorageContainer,
                    StorageContainer(
                        location.toSerLocation(),
                        ContainerCustomization(),
                        this@StorageController.block.location.toSerLocation(),
                        containerType = ContainerType.matchBlockToType(b)!!
                    ),
                )


                if (seenIds.contains(containerData.id)) continue
                seenIds.add(containerData.id)

                items.add(MenuItem(createMenuReadyItem(
                    containerData.customization.displayName ?: "<gold>${b.type}",
                    containerData.customization.description ?: listOf(
                        "<gray>Located at X: ${location.x}, Y: ${location.y}, Z: ${location.z}"
                    ),
                    containerData.customization.displayItem ?: b.type
                ).apply {
                    val meta = itemMeta
                    meta.persistentDataContainer.set(
                        Keys.itemStorageContainer,
                        PersistentDataType.STRING,
                        "${locationToString(b.location)}|${locationToString(block.location)}"
                    )
                    itemMeta = meta
                })
                { event ->
                    val state = b.state as Container
                    if (event.isLeftClick) event.whoClicked.openInventory(state.inventory)
                    if (event.isRightClick) {
                        val player = event.whoClicked as Player
                        val item = event.currentItem ?: return@MenuItem
                        val containerString = item.itemMeta.persistentDataContainer.get(
                            Keys.itemStorageContainer, PersistentDataType.STRING
                        ) ?: return@MenuItem
                        val info = parsePipedStrings(containerString)
                        val location = parseLocation(info[0]) ?: throw IllegalArgumentException(
                            "$containerString is not valid for a storage container"
                        )
                        val controllerLocation = parseLocation(info[1]) ?: throw IllegalArgumentException(
                            "$containerString is not valid for a storage container"
                        )
                        val controller = ControllerRegistry.getController(controllerLocation.chunk) ?: return@MenuItem
                        val container = controller.getContainerData().first { it.location == location }
                        player.closeInventory()
                        container.configMenu(player).open()
                    }
                })
            }

            if (toRemove.isNotEmpty()) {
                memoryChunkContainers.second[itsType]?.removeAll(toRemove.toSet())
                toRemove.forEach { rem ->
                    val ts = rem.state as? TileState
                    ts?.let {
                        it.persistentDataContainer.remove(Keys.itemStorageContainer)
                        it.update(true, false)
                    }
                }
            }
        }

        additionalItems?.let { items.addAll(it) }
        view.setItems(items)
    }

    fun openViewAsViewer(player: Player) {
        view.copy(player, displayUtilities = false).openViewOnly()
    }

    fun openViewAsUser(player: Player) {
        val menu = view.copy(player, displayUtilities = false)
        menu.blockClicks.add(ClickType.RIGHT)
        menu.open()
    }

    fun openViewAsManager(player: Player) {
        view.copy(player).open()
    }


    companion object {
        val cachedControllers: MutableMap<Pair<Int, Int>, StorageController> = mutableMapOf()

        val onlineManagers: (StorageControllerData) -> List<Player> = { data -> run {
            data.members.members.filter {
                it.value == StorageControllerMemberPermission.MANAGER
            }.toList().filter {
                Bukkit.getPlayer(it.first.toUUID()) != null
            }.map {
                Bukkit.getPlayer(it.first.toUUID())!!
            }.toMutableList().apply {
                if (Bukkit.getPlayer(data.owner.toUUID()) != null) add(Bukkit.getPlayer(data.owner.toUUID())!!)
            }.toList()
        } }

        @JvmStatic
        fun load(block: Block): StorageController {
            val blockState = block.state as TileState

            if (!blockState.persistentDataContainer.has(Keys.itemStorageController)) throw IllegalStateException("${block.location} is not capable of a storage manager ")

            val data = blockState.persistentDataContainer.get(Keys.itemStorageController, PDC.storageControllerData)
                ?: throw IllegalStateException("${block.location} is not a storage controller")

            return StorageController(
                data.owner,
                data.controllerBlock.toBlock(),
                Menu(
                    Bukkit.getPlayer(data.owner.toUUID()) ?: onlineManagers(data).first(),
                    "Storage Manager",
                    displayFilter = true
                ),
                data
            ).apply {
                cachedControllers[Pair(block.chunk.x, block.chunk.z)] = this
                updateViewState()
            }

        }

        @JvmStatic
        fun findStorageController(chunk: Chunk): StorageController? {
            val cacheHit = cachedControllers[Pair(chunk.x, chunk.z)]
            if (cacheHit != null) return cacheHit

            for (x in 0..15) for (z in 0..15) for (y in chunk.world.minHeight until chunk.world.maxHeight) {
                val block = chunk.getBlock(x, y, z)
                if (block.type == Material.LECTERN) {
                    val state = block.state as TileState
                    if (state.persistentDataContainer.has(Keys.itemStorageController)) {
                        val controller = ControllerRegistry.getController(block.location.chunk)
                        if (controller != null) {
                            cachedControllers[Pair(
                                block.location.chunk.x,
                                block.location.chunk.z)
                            ] = controller
                        }
                        return controller
                    }
                }
            }

            return null
        }
    }

}