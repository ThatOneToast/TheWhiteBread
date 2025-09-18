package dev.theWhiteBread.listeners

import dev.theWhiteBread.TheWhiteBread
import org.bukkit.event.Listener

interface BreadListener : Listener {
    fun register() = TheWhiteBread.instance.server.pluginManager.registerEvents(this, TheWhiteBread.instance)
}