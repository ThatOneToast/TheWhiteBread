package dev.theWhiteBread.items

import dev.theWhiteBread.*
import dev.theWhiteBread.listeners.BreadListener
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


open class BreadItem(
    var item: ItemStack,
    val key: String
) {
    inline fun <reified D : Any> setData(name: String, data: D) {
        item.setData(name, data)
    }

    inline fun <reified D : Any> getData(name: String): D? {
        return item.getData(name)
    }

    inline fun <reified D: Any> computeData(name: String, default: D): D {
        return item.computeData(name, default)
    }

    fun deleteData(vararg keys: String) {
        keys.forEach { item.itemMeta.persistentDataContainer.remove(Keys.create(it)) }

    }

    fun register() {
        item.apply {
            val meta = itemMeta ?: return
            meta.persistentDataContainer.set(
                Keys.create(key),
                PersistentDataType.BOOLEAN,
                true
            )
            itemMeta = meta
            this.getOrCreateId(Keys.create("id"))
        }
        listenerObject().register()
    }

    open fun onLeftClickBlock(event: PlayerInteractEvent) {}
    open fun onLeftClickAir(event: PlayerInteractEvent) {}
    open fun onRightClickBlock(event: PlayerInteractEvent) {}
    open fun onRightClickAir(event: PlayerInteractEvent) {}
    open fun onAssPressure(event: PlayerInteractEvent) {}
    /**
     * This function will run on any action.
     */
    open fun onAny(event: PlayerInteractEvent) {}

    open fun onMovement(event: PlayerMoveEvent) {}

    open fun equip(player: Player) {}
    open fun deEquip(player: Player) {}
    open fun equipOffHand(player: Player) {}
    open fun deEquipOffHand(player: Player) {}

    open fun updateItem(newState: ItemStack?) {
        if (newState == null ) return
//        val id = newState.getOrCreateId(Keys.create("id"))
//        if (item.getId(Keys.create("id")) == id) return
        item = newState
    }


    fun giveToPlayer(player: Player) {
        player.inventory.addItem(item.apply {this.setRandomId(Keys.create("id"))})
    }

    fun isMainHand(player: Player): Boolean {
        val meta = player.inventory.itemInMainHand.itemMeta ?: return false
        return meta.persistentDataContainer.get(
            Keys.create(key),
            PersistentDataType.BOOLEAN
        ) == true
    }

    fun isOffHand(player: Player): Boolean {
        val meta = player.inventory.itemInOffHand.itemMeta ?: return false
        return meta.persistentDataContainer.get(
            Keys.create(key),
            PersistentDataType.BOOLEAN
        ) == true
    }

    private fun listenerObject(): BreadListener {
        return object : BreadListener {

            private fun isThis(it: ItemStack?): Boolean {
                val meta = it?.itemMeta ?: return false
                return meta.persistentDataContainer.get(
                    Keys.create(key),
                    PersistentDataType.BOOLEAN
                ) == true
            }

            @EventHandler(ignoreCancelled = true)
            fun onClicks(event: PlayerInteractEvent) {
                if (event.action != Action.PHYSICAL && event.hand != EquipmentSlot.HAND) return

                val held = if (event.hand == EquipmentSlot.HAND)
                    event.player.inventory.itemInMainHand
                else null // PHYSICAL

                if (held != null && !isThis(held)) return
                if (held != null) updateItem(held)

                onAny(event)
                when (event.action) {
                    Action.LEFT_CLICK_BLOCK -> onLeftClickBlock(event)
                    Action.RIGHT_CLICK_BLOCK -> onRightClickBlock(event)
                    Action.LEFT_CLICK_AIR -> onLeftClickAir(event)
                    Action.RIGHT_CLICK_AIR -> onRightClickAir(event)
                    Action.PHYSICAL -> onAssPressure(event)
                }
            }

            @EventHandler
            fun movement(event: PlayerMoveEvent) {
                val item = event.player.inventory.itemInMainHand
                val offHand = event.player.inventory.itemInOffHand
                if (!isThis(item))  return
                updateItem(item)
                onMovement(event)
            }


            @EventHandler(ignoreCancelled = true)
            fun handleSwap(event: PlayerSwapHandItemsEvent) {
                // Before swap
                val beforeMain = event.player.inventory.itemInMainHand
                val beforeOff = event.player.inventory.itemInOffHand

                // After swap (what they'll hold after the event by default)
                val afterMain = event.offHandItem
                val afterOff = event.mainHandItem

                val hadMain = isThis(beforeMain)
                val hadOff = isThis(beforeOff)
                val hasMain = isThis(afterMain)
                val hasOff = isThis(afterOff)

                if (hadMain && !hasMain) {
                    updateItem(beforeMain)
                    deEquip(event.player)
                }
                if (!hadMain && hasMain) {
                    updateItem(afterMain)
                    equip(event.player)
                }

                if (hadOff && !hasOff) {
                    updateItem(beforeOff)
                    deEquipOffHand(event.player)
                }
                if (!hadOff && hasOff) {
                    updateItem(afterOff)
                    equipOffHand(event.player)
                }
            }

            // Triggered when player scrolls 1–9 (most equips)
            @EventHandler
            fun handleHeldChange(event: PlayerItemHeldEvent) {
                val inv = event.player.inventory
                val before = inv.getItem(event.previousSlot)
                val after = inv.getItem(event.newSlot)

                val had = isThis(before)
                val has = isThis(after)

                if (had && !has) {
                    updateItem(before)
                    deEquip(event.player)
                }

                if (!had && has) {
                    updateItem(after)
                    equip(event.player)
                }

            }


        }
    }

}