package dev.theWhiteBread.items

import dev.theWhiteBread.Keys
import dev.theWhiteBread.listeners.BreadListener
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

interface BreadItem {
    val item: ItemStack
    val key: String


    fun onLeftClickBlock(event: PlayerInteractEvent) {

    }

    fun onLeftClickAir(event: PlayerInteractEvent) {

    }

    fun onRightClickBlock(event: PlayerInteractEvent) {

    }

    fun onRightClickAir(event: PlayerInteractEvent) {}

    fun onAssPressure(event: PlayerInteractEvent) {}

    fun register() {
        item.apply {
            val meta = itemMeta ?: return
            meta.persistentDataContainer.set(Keys.itemAbility, PersistentDataType.STRING, key)
            itemMeta = meta
        }
        listenerObject().register()
    }

    fun giveToPlayer(player: Player) {
        player.inventory.addItem(item)
    }


    private fun listenerObject(): BreadListener {
        return object : BreadListener {

            @EventHandler
            fun onClicks(event: PlayerInteractEvent) {
                val item = event.player.inventory.itemInMainHand
                val isAbility = item.itemMeta?.persistentDataContainer?.get(Keys.itemAbility, PersistentDataType.STRING) ?: return
                if (isAbility != key) return

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