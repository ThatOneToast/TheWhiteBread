package dev.theWhiteBread.items

import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.miniMessage
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

@Suppress("UnstableApiUsage")
object ItemRegistry {
    lateinit var items: List<ItemStack>
    lateinit var menuItems: List<MenuItem>


    val dimensionalReceiver = ItemStack(Material.ENCHANTING_TABLE).apply {
        val meta = itemMeta
        meta.displayName(miniMessage.deserialize("<gold>dimensional Receiver"))
        meta.lore(listOf(
            miniMessage.deserialize("<gray>Stabilize a dimensional portal for use"),
            miniMessage.deserialize("<gray>Right click the block when placed to access configuration.")
        ))

        setData(DataComponentTypes.MAX_STACK_SIZE, 1)

        meta.setEnchantmentGlintOverride(true)
        meta.persistentDataContainer.set(Keys.dimensialReceiver, PersistentDataType.BOOLEAN, true)
        itemMeta = meta
    }

    val itemsList: Collection<ItemStack> = listOf(
        dimensionalReceiver
    )

    fun loadItemRegistry() {

        object : BukkitRunnable() {
            override fun run() {
                val allMaterials = Material.entries
                    .filter { it.isItem && it != Material.AIR && !it.name.startsWith("LEGACY_") }
                    .map { ItemStack(it) }

                items = allMaterials
                menuItems = allMaterials.map { MenuItem(it) {} }
            }
        }.runTaskAsynchronously(TheWhiteBread.instance)
    }

}