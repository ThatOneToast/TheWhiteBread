package dev.theWhiteBread.listeners

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.concurrent.ConcurrentHashMap

object Events {
    private val map = ConcurrentHashMap<Class<*>, HandlerList>()

    fun handlerListFor(clazz: Class<*>): HandlerList =
        map.computeIfAbsent(clazz) { HandlerList() }
}

abstract class BaseEvent : Event() {
    override fun getHandlers(): HandlerList = Events.handlerListFor(this::class.java)
}

abstract class BaseCancellableEvent : Event(), Cancellable {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = Events.handlerListFor(this::class.java)
}