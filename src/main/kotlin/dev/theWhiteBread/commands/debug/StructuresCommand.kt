package dev.theWhiteBread.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.commands.CommandBuilder
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import kotlin.random.Random
import kotlin.random.asJavaRandom

object StructuresCommand {

    fun register() {
        CommandBuilder()
            .setEntry("structures", null)
            .addChildWiRequiredArgument("spawn", StringArgumentType.word(), "structure_name") { ctx -> run {
                val structureName = StringArgumentType.getString(ctx, "structure_name")
                val structureFile = TheWhiteBread.instance.getResource("structures/$structureName.nbt") ?: throw IllegalStateException("No structure by that name.")
                val structure = TheWhiteBread.instance.server.structureManager.loadStructure(structureFile)
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChildWiRequiredArgument 0

                structure.place(
                    player.location.add(10.0, 10.0, 10.0),
                    true,
                    StructureRotation.NONE,
                    Mirror.NONE,
                    0,
                    1f,
                    Random.Default.asJavaRandom()
                )
                1
            }}
            .registerDebugGroup()
    }

}