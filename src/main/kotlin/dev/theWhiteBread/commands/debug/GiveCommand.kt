package dev.theWhiteBread.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.CommandSuggestionBuilder
import dev.theWhiteBread.items.ItemRegistry

object GiveCommand {
    private val items = listOf("storage_controller", "dimensional_receiver")

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
                    "storage_controller" -> player.inventory.addItem(ItemRegistry.storageController)
                    "dimensional_receiver" -> player.inventory.addItem(ItemRegistry.dimensionalReceiver)
                }
                1
            }}
            .registerDebugGroup()
    }
}