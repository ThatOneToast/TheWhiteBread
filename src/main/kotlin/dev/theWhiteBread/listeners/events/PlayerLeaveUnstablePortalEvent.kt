package dev.theWhiteBread.listeners.events

import dev.theWhiteBread.listeners.BaseCancellableEvent
import dev.theWhiteBread.listeners.Events
import dev.theWhiteBread.portals.Portal
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerLeaveUnstablePortalEvent(
    val player: Player,
    val portal: Portal
) : BaseCancellableEvent() {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(PlayerLeaveUnstablePortalEvent::class.java)
    }
}