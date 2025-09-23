package dev.theWhiteBread.commands.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.Keys
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.CommandSuggestionBuilder
import dev.theWhiteBread.getData
import dev.theWhiteBread.items.item.BuilderWand
import dev.theWhiteBread.items.item.BuilderWandMode
import dev.theWhiteBread.mapToString
import dev.theWhiteBread.message
import dev.theWhiteBread.setData
import org.bukkit.persistence.PersistentDataType

object BuilderWandCommand {
    fun register() {
        CommandBuilder()
            .setEntry("builderswand", null)
            .addChildWiRequiredArgument(
                "setmode",
                StringArgumentType.word(),
                "mode",
                suggests = CommandSuggestionBuilder()
                    .addSuggestions(BuilderWandMode.entries.map {
                        it.toString()}
                    )
                    .build()
            ) {ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChildWiRequiredArgument 0
                val mainHand = player.inventory.itemInMainHand
                if (mainHand.itemMeta.persistentDataContainer.has(Keys.create(BuilderWand.key))) {
                    val mode = BuilderWandMode.valueOf(StringArgumentType.getString(ctx, "mode"))
                    mainHand.setData("mode", mode)
                    player.message("<gray>Set mode to <gold>$mode</gold>.")
                } else return@addChildWiRequiredArgument 0

                1
            }}
            .addChildWiRequiredArgument(
                "size",
                IntegerArgumentType.integer(),
                "size",
                suggests = CommandSuggestionBuilder()
                    .addSuggestions((0..16).toList().mapToString())
                    .build()
            ) {ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChildWiRequiredArgument  0
                val mainHand = player.inventory.itemInMainHand
                if (mainHand.itemMeta.persistentDataContainer.has(Keys.create(BuilderWand.key))) {
                    val size = IntegerArgumentType.getInteger(ctx, "size")
                    mainHand.setData("size", size-1)
                    player.message("<gray>Set Builders size to <gold>${size}</gold>.")
                } else return@addChildWiRequiredArgument 0

                1
            }}
            .registerPluginGroup()
    }
}