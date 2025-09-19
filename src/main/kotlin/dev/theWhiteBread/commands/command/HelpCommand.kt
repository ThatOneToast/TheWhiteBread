package dev.theWhiteBread.commands.command

import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.debug.PortalsCommand
import dev.theWhiteBread.enchantments.enchantment.Veinminer
import dev.theWhiteBread.input.PaginatedChatMenu
import dev.theWhiteBread.message
import dev.theWhiteBread.miniMessage
import dev.theWhiteBread.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

object HelpCommand {


    enum class Commands(val description: String, val subCommands: Map<String, String>) {
        VeinMiner(
            "<gray>Customize your <green>VeinMiner</green> experience!",
            mapOf(
                "<green>disk-sweep</green>" to Veinminer.DiskSweep.entries.joinToString("\n") {
                    "<yellow>${it.name}</yellow> - <gray>${it.description}</gray>"
                },
                "<green>style</green>" to Veinminer.MinerStyle.entries.joinToString("\n") {
                    "<yellow>${it.name}</yellow> - <gray>${it.description}</gray>"
                },
                "<green>filter</green>" to Veinminer.MinerFilter.entries.joinToString("\n") {
                    "<yellow>${it.name}</yellow> - <gray>${it.description}</gray>"
                }

            )
        ),
        Location(
            "<gray><green>Save</green>, <green>Teleport</green> to <gold>locations</gold> you desire.",
            mapOf(
                "<green>save</green>" to "<gray><gold>the_name</gold> of the location you wish to save",
                "<green>delete</green>" to "<gray><gold>the_name</gold> of the location you wish to delete",
                "<green>telepeort</green>" to "<gray><gold>the_name</gold> of the location you wish to teleport to"
            )
        )

        ;

        override fun toString(): String {
            val subcommands = this.subCommands.map { (k, v) ->
                "$k: \n$v"
            }.joinToString(separator = "\n  ")
            return "<gray><green>${this.name}</green>: ${this.description}\n  $subcommands"
        }
    }

    fun register() {
        CommandBuilder()
            .setEntry("help") { ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@setEntry 0
                player.message("<gray>Welcome to <gold>WhiteBread</gold> help menu!")
                player.message("<gray>All your information in 1 spot.")
                player.message("<gray>Click on any <green>feature</green> you want more info on.")

                val menu = PaginatedChatMenu(
                    "help_menu_commands",
                    Commands.entries
                ) { item, idk ->
                    val label = Component.text("Command: ${item.name}", NamedTextColor.GREEN)
                    val description = Component.text("Click to view details", NamedTextColor.GRAY)
                    label.clickEvent(ClickEvent.callback { run {
                        player.message(item.toString())
                    } })
                        .hoverEvent(HoverEvent.showText(description))
                }

                menu.open(player)

                1
            } }
            .registerPluginGroup()
    }

}