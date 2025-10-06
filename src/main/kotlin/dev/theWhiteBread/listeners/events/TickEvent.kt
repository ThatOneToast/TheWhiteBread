package dev.theWhiteBread.listeners.events

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.listeners.BaseEvent
import dev.theWhiteBread.listeners.Events
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import java.util.UUID

class TickEvent(
    private val onlinePlayers: HashSet<UUID>,
) : BaseEvent() {

    fun getPlayer(name: String): Player? = TheWhiteBread.instance.server.getPlayer(name)
    fun getPlayer(uuid: UUID): Player? = TheWhiteBread.instance.server.getPlayer(uuid)

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList =
            Events.handlerListFor(TickEvent::class.java)
    }
}