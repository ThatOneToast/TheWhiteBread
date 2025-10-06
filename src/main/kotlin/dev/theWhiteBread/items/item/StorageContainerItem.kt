package dev.theWhiteBread.items.item

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.highlightChunkFor
import dev.theWhiteBread.items.BreadItem
import dev.theWhiteBread.listeners.events.TickEvent
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.miniMessage
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.collections.forEach

object StorageContainerItem : BreadItem(
    createMenuReadyItem(
        "<gold>Lvl 1. <blue>Storage Container",
        listOf(
            "<gray>Place this storage container in managing storage chunk",
        ),
        Material.BARREL,
        true
    ).apply {
        val meta = itemMeta
        PDC.setValueOf<Boolean>(meta.persistentDataContainer, Keys.storageContainer, true)
        itemMeta = meta
    },
    "storage_container"
) {

    init {
        computeData("show_borders", false)
    }


    override fun onTick(event: TickEvent, playersHolding: HashSet<Player>) {
        if (getData<Boolean>("show_borders")!!) {
            playersHolding.forEach {
                highlightChunkFor(
                    TheWhiteBread.instance,
                    it,
                    it.chunk,
                    durationTicks = 2,
                    spacing = 1,
                    particleSize = 2.5f
                )

            }
        }

    }

    override fun equip(player: Player) {
        setData("show_borders", true)
    }

    override fun deEquip(player: Player) {
        setData("show_borders", false)
    }
}

