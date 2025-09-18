package dev.theWhiteBread.commands.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.theWhiteBread.Keys
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.enchantments.enchantment.Veinminer
import dev.theWhiteBread.message
import dev.theWhiteBread.miniMessage
import net.minecraft.commands.CommandSourceStack
import org.bukkit.persistence.PersistentDataType


object VeinminerCommand {
    val styleSuggestion = SuggestionProvider<CommandSourceStack> { _, builder ->
        val sb = SuggestionsBuilder(builder.input, builder.start)
        Veinminer.MinerStyle.entries.forEach {
            sb.suggest(it.toString())
        }

        sb.buildFuture()
    }

    val filterSuggestion = SuggestionProvider<CommandSourceStack> { _, builder ->
        val sb = SuggestionsBuilder(builder.input, builder.start)
        Veinminer.MinerFilter.entries.forEach {
            sb.suggest(it.toString())
        }

        sb.buildFuture()
    }

    val diskSweepSuggestion = SuggestionProvider<CommandSourceStack> { _, builder ->
        val sb = SuggestionsBuilder(builder.input, builder.start)
        Veinminer.DiskSweep.entries.forEach {
            sb.suggest(it.toString())
        }

        sb.buildFuture()
    }

    fun setVeinMineStyleShape(ctx: CommandContext<CommandSourceStack>): Int {
        val playerUuid = ctx.source.player?.uuid ?: return 0
        val player = TheWhiteBread.instance.server.getPlayer(playerUuid) ?: return 0
        val pdc = player.persistentDataContainer
        val shape: String = StringArgumentType.getString(ctx, "shape")
        try {
            Veinminer.MinerStyle.valueOf(shape)
        } catch (_: Exception) {
            player.sendMessage(miniMessage.deserialize("<red>Available styles are: ${Veinminer.MinerStyle.entries}"))
            return 0
        }
        val old = pdc.get(Keys.veinMineStyle, PersistentDataType.STRING)?.split(":")?.toMutableList() ?: "Vein:Blocks:CenterOut".split(":").toMutableList()
        old[0] = shape
        pdc.set(Keys.veinMineStyle, PersistentDataType.STRING, old.joinToString(":"))
        player.message("<gray>Mining Mode: <gold>${old[0]}")
        player.message("<gray>Filter Mode: <gold>${old[1]}")
        player.message("<gray>Sweep Style: <gold>${old[2]}")
        return 1
    }

    fun setVeinMineStyleFilter(ctx: CommandContext<CommandSourceStack>): Int {
        val playerUuid = ctx.source.player?.uuid ?: return 0
        val player = TheWhiteBread.instance.server.getPlayer(playerUuid) ?: return 0
        val pdc = player.persistentDataContainer
        val filter: String = StringArgumentType.getString(ctx, "filter")
        try {
            Veinminer.MinerFilter.valueOf(filter)
        } catch (_: Exception) {
            player.sendMessage(miniMessage.deserialize("<red>Available filters are: ${Veinminer.MinerFilter.entries}"))
            return 0
        }
        val old = pdc.get(Keys.veinMineStyle, PersistentDataType.STRING)?.split(":")?.toMutableList() ?: "Vein:Blocks:CenterOut".split(":").toMutableList()
        old[1] = filter
        pdc.set(Keys.veinMineStyle, PersistentDataType.STRING, old.joinToString(":"))
        player.message("<gray>Mining Mode: <gold>${old[0]}")
        player.message("<gray>Filter Mode: <gold>${old[1]}")
        player.message("<gray>Sweep Style: <gold>${old[2]}")
        return 1
    }

    fun setVeinMineSweepStyle(ctx: CommandContext<CommandSourceStack>): Int {
        val playerUuid = ctx.source.player?.uuid ?: return 0
        val player = TheWhiteBread.instance.server.getPlayer(playerUuid) ?: return 0
        val pdc = player.persistentDataContainer
        val sweep: String = StringArgumentType.getString(ctx, "sweep")
        try {
            Veinminer.DiskSweep.valueOf(sweep)
        } catch (_: Exception) {
            player.sendMessage(miniMessage.deserialize("<red>Available sweeps are: ${Veinminer.DiskSweep.entries}"))
            return 0
        }
        val old = pdc.get(Keys.veinMineStyle, PersistentDataType.STRING)?.split(":")?.toMutableList() ?: "Vein:Blocks:CenterOut".split(":").toMutableList()
        old[2] = sweep
        pdc.set(Keys.veinMineStyle, PersistentDataType.STRING, old.joinToString(":"))
        player.message("<gray>Mining Mode: <gold>${old[0]}")
        player.message("<gray>Filter Mode: <gold>${old[1]}")
        player.message("<gray>Sweep Style: <gold>${old[2]}")
        return 1
    }


    fun register() {
        CommandBuilder()
            .setEntry("veinminer") { ctx ->
                run {
                    val player = CommandBuilder.bukkitPlayer(ctx) ?: return@setEntry 0
                    player.message("<gray>Customize your <green>Veinminer</green> within these settings")
                    player.message(
                        "<gray>Sub commands: <green>\n   Style</green> - How do we mine blocks?\n   <green>Filter</green> - The types of blocks allowed to break?\n   <green>Disk</green> - Disk style configuration."
                    )

                    1
                }
            }
            .addChildWiRequiredArgument<String>(
                "style",
                StringArgumentType.word(),
                "shape",
                suggests = styleSuggestion,
                action = ::setVeinMineStyleShape
            )
            .addChildWiRequiredArgument<String>(
                "filter",
                StringArgumentType.word(),
                "filter",
                suggests = filterSuggestion,
                action = ::setVeinMineStyleFilter
            )
            .addChildWiRequiredArgument<String>(
                "disk-sweep",
                StringArgumentType.word(),
                "sweep",
                suggests = diskSweepSuggestion,
                action = ::setVeinMineSweepStyle
            )
            .addChild("status") { ctx ->
                run {
                    val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChild 0
                    val style =
                        player.persistentDataContainer.get(Keys.veinMineStyle, PersistentDataType.STRING)?.split(":")
                            ?.toMutableList() ?: "Vein:Blocks:CenterOut".split(":").toMutableList()

                    player.message("<gray>Mining Mode: <gold>${style[0]}")
                    player.message("<gray>Filter Mode: <gold>${style[1]}")
                    player.message("<gray>Sweep Style: <gold>${style[2]}")
                    1
                }
            }
            .registerPluginGroup()
    }
}