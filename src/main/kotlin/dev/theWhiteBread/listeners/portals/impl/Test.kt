package dev.theWhiteBread.listeners.portals.impl

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.PlayerEnterPortalEvent
import dev.theWhiteBread.listeners.events.PlayerEnterUnstablePortalEvent
import dev.theWhiteBread.listeners.events.PlayerLeavePortalEvent
import dev.theWhiteBread.listeners.events.PlayerLeaveUnstablePortalEvent
import dev.theWhiteBread.listeners.events.PlayerPortalStayEvent
import dev.theWhiteBread.message
import org.bukkit.event.EventHandler

object Test : BreadListener {

    @EventHandler
    fun unStablePortal(event: PlayerEnterUnstablePortalEvent) {
        event.player.message("<green>Unstable Portal entered.")
    }

    @EventHandler
    fun unStablePortal(event: PlayerLeaveUnstablePortalEvent) {
        event.player.message("<red>Unstable Portal exited.")
    }

    @EventHandler
    fun stablePortal(event: PlayerEnterPortalEvent) {
        event.player.message("<green>Portal entered.")
    }

    @EventHandler
    fun stablePortal(event: PlayerLeavePortalEvent) {
        event.player.message("<red>Portal exited.")
    }

    @EventHandler
    fun portalTimed(event: PlayerPortalStayEvent) {
        if (event.portal.portalType.isStable()) {
            event.player.message("<gray>portal stood in ${event.thresholdSeconds} sec")
        } else if (event.portal.portalType.isUnstable()) {
            event.player.message("<red>portal stood in ${event.thresholdSeconds} sec")
        }
    }

}