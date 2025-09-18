package dev.theWhiteBread.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.CommandSuggestionBuilder
import dev.theWhiteBread.menus.presets.ColorMenu
import dev.theWhiteBread.menus.presets.CounterMenu
import dev.theWhiteBread.message

object MenuCommand {

    fun register() {
        CommandBuilder()
            .setEntry("menus", null)
            .addChildWiRequiredArgument<String>(
                "open",
                StringArgumentType.word(),
                "menu_type",
                CommandSuggestionBuilder()
                    .addSuggestions(listOf("counter", "color"))
                    .build()
            ) { ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx)!!
                when (StringArgumentType.getString(ctx, "menu_type")) {
                    "counter" -> CounterMenu(player).open()
                    "color" -> ColorMenu.open(player) { color -> run {
                        player.message("Picked color <$color>$color")
                    } }
                    else -> player.message("<gray>Possible menus are <gold>counter, color")
                }

                1
            } }
            .registerDebugGroup()
    }
}