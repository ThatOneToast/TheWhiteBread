package dev.theWhiteBread.portals

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

object PortalManager {
    private val portalsById = HashMap<String, Portal>()
    private val portalsByWorld = HashMap<UUID, MutableList<String>>()
    private val portalsByChunk = HashMap<UUID, MutableMap<Long, MutableList<String>>>()

    private fun chunkKey(x: Int, z: Int): Long =
        (x.toLong() shl 32) or (z.toLong() and 0xffffffffL)
    private val plugin = TheWhiteBread.instance

    init {
        plugin.server.worlds.forEach { world ->
            val persisted = world.persistentDataContainer
                .get(Keys.portals, PDC.portals) ?: run {
                portalsByWorld[world.uid] = ArrayList()
                portalsByChunk[world.uid] = HashMap()
                return@forEach
            }

            val worldList = ArrayList<String>(persisted.size)
            val chunkMap = HashMap<Long, MutableList<String>>()

            persisted.values.forEach { portal ->
                val extId = portal.id
                // canonical store
                portalsById[extId] = portal
                worldList.add(extId)

                val key = chunkKey(
                    portal.location.chunk.x,
                    portal.location.chunk.z
                )
                chunkMap.getOrPut(key) { ArrayList(1) }.add(extId)
            }

            portalsByWorld[world.uid] = worldList
            portalsByChunk[world.uid] = chunkMap
        }
    }

    fun registerPortal(portal: Portal, render: Boolean = false) {
        // remove any existing portal with same id (avoid duplicates in indices)
        deRegisterPortal(portal.id)

        portalsById[portal.id] = portal

        val wuid = portal.location.world.uid
        portalsByWorld.getOrPut(wuid) { ArrayList(4) }.add(portal.id)

        val key = chunkKey(
            portal.location.chunk.x,
            portal.location.chunk.z
        )
        portalsByChunk.getOrPut(wuid) { HashMap() }
            .getOrPut(key) { ArrayList(1) }
            .add(portal.id)

        if (render) portal.renderPortal()
    }

    fun deRegisterPortal(externalId: String, fromPersistence: Boolean = false): Boolean {
        val portal = portalsById.remove(externalId) ?: return false

        val wuid = portal.location.world.uid
        portalsByWorld[wuid]?.let { wl ->
            wl.remove(externalId)
            if (wl.isEmpty()) portalsByWorld.remove(wuid)
        }

        val key = chunkKey(
            portal.location.chunk.x,
            portal.location.chunk.z
        )
        portalsByChunk[wuid]?.let { cm ->
            cm[key]?.let { cl ->
                cl.remove(externalId)
                if (cl.isEmpty()) cm.remove(key)
            }
            if (cm.isEmpty()) portalsByChunk.remove(wuid)
        }

        if (fromPersistence) {
            val world = Bukkit.getWorld(wuid) ?: return false
            val portals = world.persistentDataContainer.get(Keys.portals, PDC.portals)?.toMutableMap() ?: return false
            portals.remove(externalId)
            world.persistentDataContainer.set(Keys.portals, PDC.portals, portals)
        }

        return true
    }

    fun renderPortalsForChunk(chunk: Chunk): Boolean {
        val key = chunkKey(chunk.x, chunk.z)
        val list = portalsByChunk[chunk.world.uid]?.get(key) ?: return false
        for (id in list) {
            portalsById[id]?.renderPortal()
        }
        return true
    }

    fun getPortalsInChunk(chunk: Chunk): List<Portal> {
        val key = chunkKey(chunk.x, chunk.z)
        val list = portalsByChunk[chunk.world.uid]?.get(key) ?: return emptyList()
        val out = ArrayList<Portal>(list.size)
        for (id in list) portalsById[id]?.let { out.add(it) }
        return out
    }

    fun getAllPortals(): List<Portal> = portalsById.values.toList()

    fun getPortalsNear(location: Location, chunkRadius: Int = 1): List<Portal> {
        val worldUid = location.world!!.uid
        val cx = location.chunk.x
        val cz = location.chunk.z
        val out = ArrayList<Portal>()

        // Add regular portals
        val cm = portalsByChunk[worldUid] ?: return emptyList()
        for (dx in -chunkRadius..chunkRadius) {
            for (dz in -chunkRadius..chunkRadius) {
                val key = chunkKey(cx + dx, cz + dz)
                val ids = cm[key] ?: continue
                for (id in ids) portalsById[id]?.let { out.add(it) }
            }
        }

        return out
    }

    fun getPortalById(id: String): Portal? = portalsById[id]
    fun hasPortal(id: String): Boolean = portalsById.containsKey(id)

    fun saveWorld(world: World) {
        val portals = portalsByWorld[world.uid]?.map { portalsById[it] } ?: return
        portals.forEach {
            it?.save() ?: return@forEach
        }
    }

    fun saveAll() {
        plugin.server.worlds.forEach { saveWorld(it) }
    }

    @JvmStatic
    fun load(plugin: JavaPlugin): PortalManager {
        plugin.server.worlds.forEach { world ->
            val portals = world.persistentDataContainer.get(Keys.portals, PDC.portals) ?: return@forEach
            portals.values.forEach {
                registerPortal(it, false)
            }
        }

        return PortalManager
    }
}