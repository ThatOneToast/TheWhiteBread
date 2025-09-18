package dev.theWhiteBread.storage_system

import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.storage_system.controller.StorageController
import dev.theWhiteBread.storage_system.controller.StorageControllerData
import dev.theWhiteBread.storage_system.controller.StorageControllerMembers
import dev.theWhiteBread.toSerLocation
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player

object ControllerRegistry {

    val controllers: MutableMap<Pair<Int, Int>, StorageController> = mutableMapOf()

    fun getController(chunk: Chunk): StorageController? = controllers[Pair(chunk.x, chunk.z)]

    fun getControllers(): List<StorageController> = controllers.values.toList()


    /**
     * This will force set the controller, if a controller exists here it will be overwritten with new data.
     * If this is not intended feature use `addControllerOrNull`
     */
    fun addController(owner: Player, controllerBlock: Block): StorageController {
        val chunk = Pair(controllerBlock.location.chunk.x, controllerBlock.location.chunk.z)
        controllers[chunk] = StorageController(
            owner.uniqueId.toString(),
            controllerBlock,
            Menu(owner, "<gold>${owner.displayName()}</gold> Storage Monitor").apply { setItems(emptyList()) },
            StorageControllerData(
                owner.uniqueId.toString(), controllerBlock.location.toSerLocation(), mutableMapOf(),
                StorageControllerMembers(), false, mutableSetOf()
            )
        )
        return controllers[chunk]!!

    }

    fun addControllerRaw(controller: StorageController) {
        controllers[Pair(controller.block.location.chunk.x, controller.block.location.chunk.z)] = controller
    }

    /**
     * If there is a controller already in this position this will return null
     */
    fun addControllerOrNull(owner: Player, controllerBlock: Block): StorageController? {
        val chunk = Pair(controllerBlock.location.chunk.x, controllerBlock.location.chunk.z)
        if (controllers.containsKey(chunk)) return null
        return addController(owner, controllerBlock)
    }

    /**
     * Returns null if this locations chunk it is not a storage controller
     */
    fun isController(location: Location): StorageController? {
        return controllers[Pair(location.chunk.x, location.chunk.z)] ?: StorageController.findStorageController(location.chunk)
    }

    fun isControllerCached(chunk: Chunk): Boolean {
        return controllers.contains(Pair(chunk.x, chunk.z))
    }

    fun isControllerPrecise(location: Location): StorageController? {
        val controller = controllers[Pair(location.chunk.x, location.chunk.z)] ?: return null
        return if (controller.block.location == location) controller else null
    }

    fun removeController(location: Location) {
        controllers.remove(Pair(location.chunk.x, location.chunk.z))
    }


    fun getControllerByChunk(chunk: Chunk): StorageController? {
        controllers.forEach { (location, it) -> run {
            if (location == Pair(chunk.x, chunk.z)) return it
        } }
        return null
    }


}