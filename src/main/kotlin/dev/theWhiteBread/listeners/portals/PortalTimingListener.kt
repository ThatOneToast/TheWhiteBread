package dev.theWhiteBread.listeners.portals

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.events.PlayerEnterPortalEvent
import dev.theWhiteBread.listeners.events.PlayerEnterUnstablePortalEvent
import dev.theWhiteBread.listeners.events.PlayerLeavePortalEvent
import dev.theWhiteBread.listeners.events.PlayerLeaveUnstablePortalEvent
import dev.theWhiteBread.listeners.events.PlayerPortalStayEvent
import dev.theWhiteBread.portals.PortalManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.collections.iterator

@Suppress("UNUSED")
object PortalTimingListener : BreadListener {
    private val plugin = TheWhiteBread.Companion.instance
    private val thresholdsList: List<Int> = (1..3600).toList()
    private val portalManager = PortalManager
    private val thresholds = thresholdsList.sorted()
    private val enteredAt = HashMap<UUID, MutableMap<String, Long>>()
    private val triggered = HashMap<UUID, MutableMap<String, MutableSet<Int>>>()
    private var task: BukkitTask? = null

    fun start() {
        plugin.server.pluginManager.registerEvents(this, plugin)
        if (task != null) return

        task = object : BukkitRunnable() {
            override fun run() {
                tick()
            }
        }.runTaskTimer(plugin, 20L, 20L) // start after 1s, repeat every 1s
    }

    fun stop() {
        task?.cancel()
        task = null
        HandlerList.unregisterAll(this)
        enteredAt.clear()
        triggered.clear()
    }

    @EventHandler
    private fun onEnter(e: PlayerEnterPortalEvent) {
        registerEnter(e.player.uniqueId, e.portal.id)
    }

    @EventHandler
    private fun onUnstableEnter(e: PlayerEnterUnstablePortalEvent) {
        registerEnter(e.player.uniqueId, e.portal.id)
    }

    @EventHandler
    private fun onLeave(e: PlayerLeavePortalEvent) {
        unregister(e.player.uniqueId, e.portal.id)
    }

    @EventHandler
    private fun onUnstableLeave(e: PlayerLeaveUnstablePortalEvent) {
        unregister(e.player.uniqueId, e.portal.id)
    }

    @EventHandler
    private fun onQuit(e: PlayerQuitEvent) {
        enteredAt.remove(e.player.uniqueId)
        triggered.remove(e.player.uniqueId)
    }

    private fun registerEnter(uuid: UUID, portalId: String) {
        val now = System.currentTimeMillis()
        val map = enteredAt.getOrPut(uuid) { HashMap() }
        if (map.containsKey(portalId)) return
        map[portalId] = now
        triggered.getOrPut(uuid) { HashMap() }[portalId] = HashSet()
    }

    private fun unregister(uuid: UUID, portalId: String) {
        enteredAt[uuid]?.remove(portalId)
        if (enteredAt[uuid]?.isEmpty() == true) enteredAt.remove(uuid)
        triggered[uuid]?.remove(portalId)
        if (triggered[uuid]?.isEmpty() == true) triggered.remove(uuid)
    }

    private fun tick() {
        if (enteredAt.isEmpty()) return
        val now = System.currentTimeMillis()

        // snapshot to avoid concurrent modification when we call events
        val snapshot = HashMap<UUID, Map<String, Long>>(enteredAt.size)
        for ((u, m) in enteredAt) snapshot[u] = HashMap(m)

        for ((uuid, portals) in snapshot) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            for ((portalId, enteredMillis) in portals) {
                val elapsedSec = ((now - enteredMillis) / 1000).toInt()
                for (th in thresholds) {
                    if (elapsedSec < th) break
                    val trigMap = triggered[uuid]?.get(portalId) ?: continue
                    if (trigMap.contains(th)) continue

                    val portal = portalManager.getPortalById(portalId) ?: continue
                    val evt = PlayerPortalStayEvent(player, portal, th, elapsedSec)
                    Bukkit.getPluginManager().callEvent(evt)
                    if (!evt.isCancelled) trigMap.add(th)
                }
            }
        }
    }
}