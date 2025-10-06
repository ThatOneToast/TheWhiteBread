package dev.theWhiteBread.items.item

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.highlightChunkFor
import dev.theWhiteBread.items.BreadItem
import dev.theWhiteBread.listeners.events.TickEvent
import dev.theWhiteBread.menus.createMenuReadyItem
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
object StorageManagerItem : BreadItem(
    createMenuReadyItem(
        "<gold>Storage Controller</gold>",
        listOf(
            "<gray>Place this storage controller in any",
            "<gray>chunk to unify your storage containers into 1!",
        ),
        Material.CRAFTER,
        true
    ).apply {
        val meta = itemMeta
        PDC.setValueOf<Boolean>(meta.persistentDataContainer, Keys.storageController, true)
        setData(DataComponentTypes.MAX_STACK_SIZE, 1)
        meta.setMaxStackSize(1)
        itemMeta = meta
    },
    "storage_manager_item"
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