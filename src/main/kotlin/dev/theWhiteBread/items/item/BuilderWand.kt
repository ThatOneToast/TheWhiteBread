package dev.theWhiteBread.items.item

import dev.theWhiteBread.items.BreadItem
import dev.theWhiteBread.message
import dev.theWhiteBread.toComponent
import org.bukkit.Material
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object BuilderWand : BreadItem {
    override val key: String = "builders_wand"

    override val item: ItemStack = ItemStack(Material.STICK).apply {
        val meta = itemMeta
        meta.displayName("<rainbow>This is the name i choose".toComponent())

        itemMeta = meta
    }


    override fun onLeftClickAir(event: PlayerInteractEvent) {
        event.player.message("<green>You left clicked air!!")
    }

    override fun onRightClickAir(event: PlayerInteractEvent) {
        event.player.message("<gold>You right clicked air!!")
    }

}