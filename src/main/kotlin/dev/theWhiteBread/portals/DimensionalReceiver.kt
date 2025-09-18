package dev.theWhiteBread.portals

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.serializables.SerializableLocation
import kotlinx.serialization.Serializable
import org.bukkit.block.TileState

@Serializable
data class DimensionalReceiver(
    val location: SerializableLocation,
    val block: SerializableLocation,
    var color: String,
    val id: String,

    var isUnstable: Boolean = true
) {
    fun save() {
        val block = block.toBlock().state as TileState
        block.persistentDataContainer.set(Keys.dimensialReceiver, PDC.dimensialReceiver, this)
    }
}