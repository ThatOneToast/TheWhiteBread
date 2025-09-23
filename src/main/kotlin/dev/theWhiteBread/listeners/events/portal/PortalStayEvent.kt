package dev.theWhiteBread.listeners.events.portal

import dev.theWhiteBread.listeners.BaseCancellableEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.portals.portal.Portal
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerPortalStayEvent(
    val player: Player,
    val portal: Portal,
    val thresholdSeconds: Int,
    val elapsedSeconds: Int
) : BaseCancellableEvent() {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(PlayerPortalStayEvent::class.java)
    }
}