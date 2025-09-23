package dev.theWhiteBread

import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.commands.BreadedCommandMap
import dev.theWhiteBread.commands.command.BuilderWandCommand
import dev.theWhiteBread.commands.command.HelpCommand
import dev.theWhiteBread.commands.command.LocationsCommand
import dev.theWhiteBread.commands.command.VeinminerCommand
import dev.theWhiteBread.commands.debug.*
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.items.item.BuilderWand
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.RecipesListener
import dev.theWhiteBread.listeners.enchantments.AutoSmeltListener
import dev.theWhiteBread.listeners.enchantments.EXPBoostListener
import dev.theWhiteBread.listeners.enchantments.VeinminerListener
import dev.theWhiteBread.listeners.input.ChatInput
import dev.theWhiteBread.listeners.menus.MenuListener
import dev.theWhiteBread.listeners.storage_system.controller.SMEventCallerListener
import dev.theWhiteBread.listeners.storage_system.controller.events.Test
import dev.theWhiteBread.recipes.recipe.StorageControllerRecipe
import dev.theWhiteBread.storage_manager.StorageRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class TheWhiteBread : JavaPlugin() {

    override fun onLoad() {
        instance = this
        pluginLogger = PluginLogger(this)
        database = Database().database


        if (AREWEDEBUGGING) {
            for (i in 1..25) {
                pluginLogger.warning("WE ARE IN DEBUG MODE")
            }
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        ItemRegistry.loadItemRegistry()


//        Test.register()


        // ------------------ Command Registration ------------------
        VeinminerCommand.register()
        LocationsCommand.register()
        StructuresCommand.register()
        MenuCommand.register()
        GiveCommand.register()
        PortalsCommand.register()
        ChatMenuCommand.register()
        HelpCommand.register()
        BuilderWandCommand.register()

        // Registers grouped commands
        BreadedCommandMap.loadCommands()

        // ------------------ Listener Registration ------------------
//        PortalDetectionListener.register(); PortalTimingListener.register(); PortalTimingListener.start(); PortalUtilsListener.register()
//        DimensionalPlacementsListener.register(); DimensionalAccessListener.register()

        BuilderWand.register()

        VeinminerListener.register()
        AutoSmeltListener.register()
        EXPBoostListener.register()

        MenuListener.register()
        ChatInput.register()

        RecipesListener.register()

        SMEventCallerListener.register()
        Test.register()

        // ------------------ Recipe Registration ------------------
        StorageControllerRecipe.computeRecipe(this)

//        server.scheduler.scheduleSyncRepeatingTask(this, {
//            PortalManager.getAllPortals().forEach { portal ->
//                portal.renderPortal()
//            }
//        }, 0, 2)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        StorageRegistry.storageManagers.save()
//        PortalManager.saveAll()
//        PortalTimingListener.stop()
        threadingScopes.pluginScope.cancel()
        threadingScopes.bukkitDispatcher.cancel()
        threadingScopes.migrationDispatcher.cancel()
    }


    companion object {

        @JvmStatic
        lateinit var instance: JavaPlugin

        @JvmStatic
        lateinit var pluginLogger: PluginLogger

        @JvmStatic
        lateinit var database: HikariDataSource

        @JvmStatic
        val threadingScopes: ThreadingScopes = ThreadingScopes(Job())

    }
}
