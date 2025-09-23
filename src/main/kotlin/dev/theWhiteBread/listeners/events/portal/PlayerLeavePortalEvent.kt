package dev.theWhiteBread.listeners.events.portal

import dev.theWhiteBread.listeners.BaseCancellableEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.portals.portal.Portal
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerLeavePortalEvent(
    val player: Player,
    val portal: Portal
) : BaseCancellableEvent() {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(PlayerLeavePortalEvent::class.java)
    }
}