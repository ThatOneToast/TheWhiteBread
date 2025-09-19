package dev.theWhiteBread.commands.debug

import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.input.PaginatedChatMenu
import dev.theWhiteBread.message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor


object ChatMenuCommand {
        
    fun register() {
        CommandBuilder()
            .setEntry("chat_menu") { ctx -> run {
                
                1
            } }
            .addChild("open_test") { ctx ->
                run {
                    val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChild 0
                    val sampleItems = (1..20).map { "Item #$it" }

                    val menu = PaginatedChatMenu(
                        id = "sample",
                        items = sampleItems,
                    ) { item, idx ->
                        // each item clickable to run a command that uses its absolute index (example)
                        val label = Component.text(item, NamedTextColor.WHITE)
                        val description = Component.text("Click to view details", NamedTextColor.GRAY)
                        label.clickEvent(ClickEvent.callback { run {
                            player.message("<gray>Clicked <gold>$item</gold> item")
                        } })
                            .hoverEvent(HoverEvent.showText(description))
                    }

                    menu.open(player)
                    1
                }
            }
            .registerDebugGroup()
    }
}