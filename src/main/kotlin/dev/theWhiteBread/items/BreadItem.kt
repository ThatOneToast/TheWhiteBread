package dev.theWhiteBread.items

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.listeners.BreadListener
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.ListPersistentDataType
import org.bukkit.persistence.PersistentDataType



open class BreadItem(
    val item: ItemStack,
    val key: String
) {

    inline fun <reified D : Any> setData(name: String, data: D) {
        val ns = Keys.create(name)
        val meta = item.itemMeta!!

        val json = PDC.defaultJson.encodeToString(serializer<D>(), data)
        meta.persistentDataContainer.set(ns, PersistentDataType.BYTE_ARRAY, json.toByteArray(Charsets.UTF_8))

        item.itemMeta = meta
    }

    inline fun <reified D : Any> getData(name: String): D? {
        val ns = Keys.create(name)

        val meta = item.itemMeta ?: return null
        val bytes = meta.persistentDataContainer.get(ns, PersistentDataType.BYTE_ARRAY) ?: return null
        val json = bytes.toString(Charsets.UTF_8)

        val value = PDC.defaultJson.decodeFromString(serializer<D>(), json)
        return value
    }

    fun deleteData(key: String) = item.itemMeta.persistentDataContainer.remove(Keys.create(key))

    fun register() {
        item.apply {
            val meta = itemMeta ?: return
            meta.persistentDataContainer.set(
                Keys.create(key),
                PersistentDataType.BOOLEAN,
                true
            )
            itemMeta = meta
        }
        listenerObject().register()
    }


    open fun onLeftClickBlock(event: PlayerInteractEvent) {}
    open fun onLeftClickAir(event: PlayerInteractEvent) {}
    open fun onRightClickBlock(event: PlayerInteractEvent) {}
    open fun onRightClickAir(event: PlayerInteractEvent) {}
    open fun onAssPressure(event: PlayerInteractEvent) {}


    fun giveToPlayer(player: Player) {
        player.inventory.addItem(item)
    }

    private fun listenerObject(): BreadListener {
        return object : BreadListener {
            @EventHandler
            fun onClicks(event: PlayerInteractEvent) {
                val item = event.player.inventory.itemInMainHand
                val isAbility = item.itemMeta?.persistentDataContainer?.get(Keys.create(key), PersistentDataType.BOOLEAN) ?: false
                if (!isAbility) return

                when (event.action) {
                    Action.LEFT_CLICK_BLOCK -> onLeftClickBlock(event)
                    Action.RIGHT_CLICK_BLOCK -> onRightClickBlock(event)
                    Action.LEFT_CLICK_AIR -> onLeftClickAir(event)
                    Action.RIGHT_CLICK_AIR -> onRightClickAir(event)
                    Action.PHYSICAL -> onAssPressure(event)
                }
            }

        }
    }



}