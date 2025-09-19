package dev.theWhiteBread.commands.debug

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.theWhiteBread.Keys
import dev.theWhiteBread.PDC
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.commands.CommandArgument
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.commands.CommandSuggestionBuilder
import dev.theWhiteBread.commands.getOptionalArgument
import dev.theWhiteBread.message
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.portals.portal.BreachPortal
import dev.theWhiteBread.portals.portal.Portal
import dev.theWhiteBread.portals.portal.UnstableBreachPortal
import dev.theWhiteBread.roundTo2Place
import dev.theWhiteBread.toLocation
import kotlinx.coroutines.launch
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.world.phys.Vec3
import kotlin.coroutines.coroutineContext

object PortalsCommand {

    fun register() {
        CommandBuilder()
            .setEntry("portals") { ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@setEntry 0
                val portals = player.world.persistentDataContainer.get(Keys.portals, PDC.portals) ?: emptyMap()
                val msg = StringBuilder()
                    .apply {
                        portals.forEach { (id, portal) -> run {
                            append("<gray><green>$id</green>: <gold>X:</gold> ${portal.location.x} <gold>Y:</gold> ${portal.location.y} <gold>Z:</gold> ${portal.location.z}")
                            append("\n<gray>   <gold>isStable</gold>: ${portal.portalType.isStable()}")
                            append("\n<gray>   <gold>persistent</gold>: ${portal.persistence}")
                            append("\n--- --- --- --- --- ---" +
                                    "\n")
                        }}
                    }
                    .toString()
                player.message(msg)
                1

            }}
            .addChild("forceLoad") {ctx -> run {
                PortalManager.load(TheWhiteBread.instance)
                CommandBuilder.bukkitPlayer(ctx)?.message("<gray>Reloaded portals")
                1
            }}
            .addChildWiRequiredArgument(
                "removePersisted",
                StringArgumentType.word(),
                "to_remove",
                suggests = run {
                    CommandSuggestionBuilder().apply {
                        TheWhiteBread.threadingScopes.pluginScope.launch(TheWhiteBread.threadingScopes.bukkitDispatcher) {
                            TheWhiteBread.instance.server.worlds.forEach {
                                val portals = it.persistentDataContainer.get(Keys.portals, PDC.portals)?.keys ?: return@forEach
                                this@apply.addSuggestions(portals)
                            }
                        }
                    }.build()
                }

            ) {ctx -> run {
                val removed = PortalManager.deRegisterPortal(StringArgumentType.getString(ctx, "to_remove"), true)
                PortalManager.saveAll()
                if (removed) CommandBuilder.bukkitPlayer(ctx)?.message("<gray>Removed portal from the universe")
                else CommandBuilder.bukkitPlayer(ctx)?.message("<red>Portal doesn't exist.")
                1
            }}
            .addChildWithArguments(
                "spawn",
                listOf(
                    CommandArgument("portal_id", StringArgumentType.word()),
                    CommandArgument("is_persisted", BoolArgumentType.bool()),
                    CommandArgument("stable", BoolArgumentType.bool()),
                    CommandArgument("portal_location", Vec3Argument.vec3()),
                    CommandArgument(
                        "color",
                        StringArgumentType.word(),
                        CommandSuggestionBuilder()
                            .addSuggestions(listOf("green", "red"))
                            .build()
                    ),
                    CommandArgument("portal_destination", Vec3Argument.vec3()),
                ),
                attachExecutesToEachNode = true
            ) { ctx -> run {
                val player = CommandBuilder.bukkitPlayer(ctx) ?: return@addChildWithArguments 0
                val portalID = StringArgumentType.getString(ctx, "portal_id")
                val isStable = ctx.getOptionalArgument<Boolean>("stable") ?: false
                val location = ctx.getOptionalArgument<Vec3>("portal_location")?.roundTo2Place() ?: ctx.source.position.roundTo2Place().add(Vec3(0.0, 5.0, 0.0)).roundTo2Place()
                val destinationLocation = ctx.getOptionalArgument<Vec3>("portal_destination")?.roundTo2Place()
                val color = ctx.getOptionalArgument<String>("color") ?: "0"
                val isPersisted = ctx.getOptionalArgument<Boolean>("is_persisted") ?: false

                val portal: Portal = when(isStable) {
                    true -> BreachPortal(portalID, player.uniqueId.toString(), location.toLocation(player.location.world.uid), destinationLocation?.toLocation(player.location.world.uid), persistence = isPersisted, color = color.toInt())
                    false -> UnstableBreachPortal(portalID, player.uniqueId.toString(), location.toLocation(player.location.world.uid), destinationLocation?.toLocation(player.location.world.uid), persistence = isPersisted)
                }
                PortalManager.registerPortal(portal, true)
                portal.save()
                player.message("<gray>Created a portal!")
                1
            }}
            .registerDebugGroup()
    }
}