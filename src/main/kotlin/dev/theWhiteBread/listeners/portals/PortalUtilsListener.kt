package dev.theWhiteBread.listeners.portals

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.portals.PortalManager
import org.bukkit.event.EventHandler
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

object PortalUtilsListener : BreadListener {

    @EventHandler
    fun renderPortals(event: WorldLoadEvent) {
        PortalManager.loadWorldsPortals(TheWhiteBread.instance, event.world.uid)
    }

    @EventHandler
    fun stopRenderingPortals(event: WorldUnloadEvent) {
        PortalManager.unLoadWorldsPortals(TheWhiteBread.instance, event.world.uid)
    }
}