package dev.theWhiteBread.portals.portal

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.portals.PortalType
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.BoundingBox
import kotlin.collections.toMutableMap

@Serializable
sealed interface Portal {
    fun save() {
        if (!persistence) return
        val world = Bukkit.getWorld(this.location.world.uid)!!
        val portals = world.persistentDataContainer.get(Keys.portals, PDC.portals)?.toMutableMap() ?: mutableMapOf()
        portals[this.id] = this
        TheWhiteBread.pluginLogger.info("Saved portal $id")
        world.persistentDataContainer.set(
            Keys.portals,
            PDC.portals,
            portals.toMap()
        )
    }

    fun renderPortal()

    val id: String
    val portalType: PortalType
    val location: Location
    val destination: Location?
    var persistence: Boolean

    // axis aligned bounding box (configurable vertical extent)
    fun boundingBox(height: Double = 3.0): BoundingBox

    // tests using AABB
    fun containsAabb(loc: Location, height: Double = 3.0): Boolean =
        loc.world?.uid == location.world.uid && boundingBox(height).contains(
            loc.x, loc.y, loc.z
        )

}