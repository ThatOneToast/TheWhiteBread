package dev.theWhiteBread.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.CommandSuggestionBuilder
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.items.item.BuilderWand
import dev.theWhiteBread.items.item.StorageContainerItem
import dev.theWhiteBread.items.item.StorageManagerItem

object GiveCommand {
    private val items = listOf("storage_controller", "dimensional_receiver", "builders_wand", "storage_container")

    fun register() {
        CommandBuilder()
            .setEntry("give", null)
            .addChildWiRequiredArgument<String>(
                "item",
                StringArgumentType.word(),
                "item_name",
                CommandSuggestionBuilder()
                    .addSuggestions(items)
                    .build()

            ) { ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx)!!
                when (StringArgumentType.getString(ctx, "item_name")) {
                    "storage_controller" -> player.inventory.addItem(StorageManagerItem.item)
                    "dimensional_receiver" -> player.inventory.addItem(ItemRegistry.dimensionalReceiver)
                    "builders_wand" -> player.inventory.addItem(BuilderWand.item)
                    "storage_container" -> player.inventory.addItem(StorageContainerItem.item)
                }
                1
            }}
            .registerDebugGroup()
    }
}