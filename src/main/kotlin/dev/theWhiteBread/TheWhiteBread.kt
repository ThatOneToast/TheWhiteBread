package dev.theWhiteBread

import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.commands.BreadedCommandMap
import dev.theWhiteBread.commands.command.HelpCommand
import dev.theWhiteBread.commands.command.LocationsCommand
import dev.theWhiteBread.commands.command.VeinminerCommand
import dev.theWhiteBread.commands.debug.ChatMenuCommand
import dev.theWhiteBread.commands.debug.GiveCommand
import dev.theWhiteBread.commands.debug.MenuCommand
import dev.theWhiteBread.commands.debug.PortalsCommand
import dev.theWhiteBread.commands.debug.StructuresCommand
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.items.item.BuilderWand
import dev.theWhiteBread.listeners.BreadListener
import dev.theWhiteBread.listeners.RecipesListener
import dev.theWhiteBread.listeners.enchantments.AutoSmeltListener
import dev.theWhiteBread.listeners.enchantments.EXPBoostListener
import dev.theWhiteBread.listeners.enchantments.VeinminerListener
import dev.theWhiteBread.listeners.input.ChatInput
import dev.theWhiteBread.listeners.menus.MenuListener
import dev.theWhiteBread.listeners.portals.DimensionalAccessListener
import dev.theWhiteBread.listeners.portals.DimensionalPlacementsListener
import dev.theWhiteBread.listeners.portals.PortalDetectionListener
import dev.theWhiteBread.listeners.portals.impl.Test
import dev.theWhiteBread.listeners.storage_system.container.ContainerPlacementListeners
import dev.theWhiteBread.listeners.storage_system.controller.ControllerAccessListener
import dev.theWhiteBread.listeners.storage_system.controller.ControllerPlacementsListener
import dev.theWhiteBread.listeners.storage_system.controller.LoadingOfControllersListener
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.listeners.portals.PortalTimingListener
import dev.theWhiteBread.listeners.portals.PortalUtilsListener
import dev.theWhiteBread.recipes.recipe.StorageControllerRecipe
import dev.theWhiteBread.storage_system.container.ItemTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class TheWhiteBread : JavaPlugin(), BreadListener {

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

    @EventHandler
    private fun stuff(event: PlayerJoinEvent) {
        BuilderWand.giveToPlayer(event.player)
    }

    override fun onEnable() {
        // Plugin startup logic
        ItemRegistry.loadItemRegistry()

        ItemTransport.storageManagerPreparation.runTaskTimer(this, 0, 1200L)
        ItemTransport.storageManagerInvExecution.runTaskTimer(this, 60L, 1200L)

//        Test.register()

        // ------------------ Item Ability Registration ------------------
        BuilderWand.register()

        // ------------------ Command Registration ------------------
        VeinminerCommand.register()
        LocationsCommand.register()
        StructuresCommand.register()
        MenuCommand.register()
        GiveCommand.register()
        PortalsCommand.register()
        ChatMenuCommand.register()
        HelpCommand.register()

        // Registers grouped commands
        BreadedCommandMap.loadCommands()

        // ------------------ Listener Registration ------------------
        this.register()

        LoadingOfControllersListener.register(); ControllerPlacementsListener.register(); ControllerAccessListener.register(); ContainerPlacementListeners.register()
//        PortalDetectionListener.register(); PortalTimingListener.register(); PortalTimingListener.start(); PortalUtilsListener.register()
//        DimensionalPlacementsListener.register(); DimensionalAccessListener.register()


        VeinminerListener.register()
        AutoSmeltListener.register()
        EXPBoostListener.register()

        MenuListener.register()
        ChatInput.register()

        RecipesListener.register()

        // ------------------ Recipe Registration ------------------
        StorageControllerRecipe.computeRecipe(this)

        server.scheduler.scheduleSyncRepeatingTask(this, {
            PortalManager.getAllPortals().forEach { portal ->
                portal.renderPortal()
            }
        }, 0, 2)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        PortalManager.saveAll()
        PortalTimingListener.stop()
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
