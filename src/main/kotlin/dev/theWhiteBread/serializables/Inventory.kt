package dev.theWhiteBread.serializables

import dev.theWhiteBread.toData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

@Serializable
data class InventoryData(
    val contents: Map<Int, ByteArray>,
    val size: Int,
) {

    fun toInventory(): Inventory {
        val inv = Bukkit.createInventory(null, size)
        contents.forEach {
            inv.setItem(it.key, ItemStack.deserializeBytes(it.value))
        }

        return inv
    }
}

object InventorySerializer : KSerializer<Inventory> {
    private val delegate = InventoryData.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Inventory) {
        val data: InventoryData = value.toData()
        encoder.encodeSerializableValue(delegate, data)
    }

    override fun deserialize(decoder: Decoder): Inventory {
        val data = decoder.decodeSerializableValue(delegate)
        return data.toInventory()
    }

}