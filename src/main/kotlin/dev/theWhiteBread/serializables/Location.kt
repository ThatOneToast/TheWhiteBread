package dev.theWhiteBread.serializables

import dev.theWhiteBread.chunkKey
import dev.theWhiteBread.packInts
import dev.theWhiteBread.toUUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.UUID

@Serializable
data class SerializableLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val chunkKey: Long = Location(Bukkit.getWorld(UUID.fromString(world)), x, y, z).chunkKey()
) {

    companion object {
        @JvmStatic
        fun toSerialized(location: Location): SerializableLocation {
            return SerializableLocation(
                location.world.uid.toString(),
                location.x,
                location.y,
                location.z,
                packInts(
                    location.chunk.x,
                    location.chunk.z)
            )
        }
    }

    fun fromSerLocation(): Location{
        return Location(Bukkit.getWorld(this.world.toUUID()), this.x, this.y, this.z)
    }

    fun toBlock(): Block = fromSerLocation().block

}

object LocationSerializer : KSerializer<Location> {
    private val delegate = SerializableLocation.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Location) {
        val ser = SerializableLocation.toSerialized(value)
        encoder.encodeSerializableValue(delegate, ser)
    }

    override fun deserialize(decoder: Decoder): Location {
        val ser = decoder.decodeSerializableValue(delegate)
        return ser.fromSerLocation()
    }
}