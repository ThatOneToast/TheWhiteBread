package dev.theWhiteBread.listeners.storage_system.controller

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.controller.StorageController
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.collections.forEach

object LoadingOfControllersListener : BreadListener {

    @EventHandler
    fun loadControllers(event: PlayerJoinEvent) {
        val pdc = event.player.persistentDataContainer
        if (pdc.has(Keys.playerStorageControllers)) {
            val data = pdc.get(Keys.playerStorageControllers, PDC.playerStorageControllerList) ?: emptyList()
            data.forEach {
                val location = it.fromSerLocation()
                val controller = StorageController.load(location.block)
                controller.updateViewState()
                ControllerRegistry.addControllerRaw(controller)
            }
        }


        if (pdc.has(Keys.itemStorageControllerManagerOf)) {
            val data = pdc.get(Keys.itemStorageControllerManagerOf, PDC.playerManagerOfStorageControllers)!!.map { it.fromSerLocation() }
            data.forEach {
                if (!ControllerRegistry.isControllerCached(it.chunk)) {
                    val controller = StorageController.findStorageController(it.chunk) ?: return@forEach
                    controller.updateViewState()
                    ControllerRegistry.addControllerRaw(controller)
                }

            }
        }
    }

    @EventHandler
    fun unLoadControllers(event: PlayerQuitEvent) {
        val pdc = event.player.persistentDataContainer

        if (pdc.has(Keys.playerStorageControllers)) {
            val data = pdc.get(Keys.playerStorageControllers, PDC.playerStorageControllerList)!!
            val controllers = data.map { ControllerRegistry.getController(it.fromSerLocation().chunk)!!}
            controllers.forEach {
                if (StorageController.onlineManagers(it.data).isNotEmpty()) return@forEach
                it.save()
                ControllerRegistry.removeController(it.block.location)
            }
        }

        if (pdc.has(Keys.itemStorageControllerManagerOf)) {
            val data = pdc.get(Keys.itemStorageControllerManagerOf, PDC.playerManagerOfStorageControllers)!!
            data.forEach {
                val controller = ControllerRegistry.getController(it.fromSerLocation().chunk) ?: return@forEach
                if (controller.isOwnerOnline()) return@forEach
                if (StorageController.onlineManagers(controller.data).isNotEmpty()) return@forEach
                controller.save()
                ControllerRegistry.removeController(it.fromSerLocation())

            }
        }
    }

}