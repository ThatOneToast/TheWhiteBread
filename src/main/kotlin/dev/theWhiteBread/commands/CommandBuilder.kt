package dev.theWhiteBread.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.theWhiteBread.AREWEDEBUGGING
import dev.theWhiteBread.TheWhiteBread
import net.minecraft.commands.CommandSourceStack
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.entity.Player

object BreadedCommandMap {
    val pluginGroupedCommands: MutableSet<LiteralArgumentBuilder<CommandSourceStack>> = mutableSetOf()
    val debugGroupedCommands: MutableSet<LiteralArgumentBuilder<CommandSourceStack>> = mutableSetOf()


    fun loadCommands() {
        val commands = LiteralArgumentBuilder.literal<CommandSourceStack>("whitebread")
            .apply {
                pluginGroupedCommands.forEach { then(it) }
                if (AREWEDEBUGGING) {
                    then(LiteralArgumentBuilder.literal<CommandSourceStack>("DEBUG").apply {
                        debugGroupedCommands.forEach { then(it) }
                    })
                }

            }
        val dispatcher = (TheWhiteBread.instance.server as CraftServer).server.commands.dispatcher
        dispatcher.register(commands)
        TheWhiteBread.pluginLogger.info("WhiteBread: Brigadier command(s) '/whitebread' registered")

    }
}

@Suppress("Unused")
class CommandSuggestionBuilder() {
    private val suggestions: MutableSet<String> = mutableSetOf()

    fun addSuggestion(suggestion: String): CommandSuggestionBuilder {
        suggestions.add(suggestion)
        return this
    }

    fun addSuggestions(suggestions: Collection<String>): CommandSuggestionBuilder {
        this.suggestions.addAll(suggestions)
        return this
    }

    fun build() = SuggestionProvider<CommandSourceStack> {_, builder ->
        val sb = SuggestionsBuilder(builder.input, builder.start)
        suggestions.forEach { sb.suggest(it) }
        sb.buildFuture()
    }
}

@Suppress("Unused")
class CommandBuilder {
    private lateinit var command: LiteralArgumentBuilder<CommandSourceStack>

    fun setEntry(entryName: String, action: ((CommandContext<CommandSourceStack>) -> Int)?): CommandBuilder {
        command = LiteralArgumentBuilder.literal<CommandSourceStack>(entryName)
            .apply {
                if (action != null) {
                    this.executes(action)
                }
            }
        return this
    }

    fun addChild(childName: String, action: (CommandContext<CommandSourceStack>) -> Int): CommandBuilder {
        val child = LiteralArgumentBuilder.literal<CommandSourceStack>(childName)
            .executes(action)
        command = command.then(child)
        return this
    }

    fun <T> addChildWiRequiredArgument(
        childName: String,
        argumentType: ArgumentType<T>,
        argumentName: String,
        suggests: SuggestionProvider<CommandSourceStack>? = null,
        action: (CommandContext<CommandSourceStack>) -> Int
    ): CommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandSourceStack, T>(
            argumentName, argumentType
        ).apply {
            if (suggests != null) this.suggests(suggests)
            this.executes(action)
        } as ArgumentBuilder<CommandSourceStack, *>

        val child = LiteralArgumentBuilder.literal<CommandSourceStack>(childName)
            .then(arg)

        command = command.then(child)
        return this
    }

    fun register() {
        val dispatcher = (TheWhiteBread.instance.server as CraftServer).server.commands.dispatcher
        dispatcher.register(command)
    }

    fun registerPluginGroup() {
        BreadedCommandMap.pluginGroupedCommands.add(command)
    }

    fun registerDebugGroup() {
        BreadedCommandMap.debugGroupedCommands.add(command)
    }

    companion object {

        @JvmStatic
        fun bukkitPlayer(ctx: CommandContext<CommandSourceStack>): Player? =
            ctx.source.player?.uuid?.let { TheWhiteBread.instance.server.getPlayer(it) }
    }
}