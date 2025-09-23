package dev.theWhiteBread.listeners.portals

import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.portal.PlayerEnterPortalEvent
import dev.theWhiteBread.listeners.events.portal.PlayerEnterUnstablePortalEvent
import dev.theWhiteBread.listeners.events.portal.PlayerLeavePortalEvent
import dev.theWhiteBread.listeners.events.portal.PlayerLeaveUnstablePortalEvent
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.portals.PortalType
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

object PortalDetectionListener : BreadListener {
    private val inside = mutableMapOf<UUID, MutableSet<String>>()

    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val player = e.player
        val to = e.to
        val from = e.from

        // micro-opt: ignore movement inside same block (reduce checks)
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

        val pm = PortalManager
        val nearby = pm.getPortalsNear(to, chunkRadius = 1)

        val nowInside = mutableSetOf<String>()
        for (portal in nearby) {
            if (portal.containsAabb(to)) nowInside.add(portal.id)
        }

        val prev = inside.getOrPut(player.uniqueId) { mutableSetOf() }

        val entered = nowInside - prev
        val left = prev - nowInside

        if (entered.isNotEmpty()) {
            for (id in entered) {
                val portal = pm.getPortalById(id) ?: continue
                when (portal.portalType) {
                    PortalType.STABLE -> {
                        val evt = PlayerEnterPortalEvent(player, portal)
                        Bukkit.getPluginManager().callEvent(evt)
                    }

                    PortalType.UNSTABLE -> {
                        val evt = PlayerEnterUnstablePortalEvent(player, portal)
                        Bukkit.getPluginManager().callEvent(evt)
                    }
                }
            }
        }

        if (left.isNotEmpty()) {
            for (id in left) {
                val portal = pm.getPortalById(id) ?: continue
                when (portal.portalType) {
                    PortalType.STABLE -> {
                        val evt = PlayerLeavePortalEvent(player, portal)
                        Bukkit.getPluginManager().callEvent(evt)
                    }

                    PortalType.UNSTABLE -> {
                        val evt = PlayerLeaveUnstablePortalEvent(player, portal)
                        Bukkit.getPluginManager().callEvent(evt)
                    }
                }
            }
        }

        inside[player.uniqueId] = nowInside
    }
}