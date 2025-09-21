package dev.theWhiteBread.items.item

import dev.theWhiteBread.PDC
import dev.theWhiteBread.items.BreadItem
import dev.theWhiteBread.message
import dev.theWhiteBread.toComponent
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack


object BuilderWand : BreadItem(
    ItemStack(Material.STICK).apply {
        val meta = itemMeta
        meta.displayName("<rainbow>This is the name i choose".toComponent())

        itemMeta = meta
    },
    "builders_wand"
) {
    init {
    }

    override fun onLeftClickAir(event: PlayerInteractEvent) {
        event.player.message("<green>You left clicked air!!")
    }

    override fun onRightClickAir(event: PlayerInteractEvent) {
        event.player.message("<gold>You right clicked air!!")
    }

}