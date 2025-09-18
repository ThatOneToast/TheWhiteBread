package dev.theWhiteBread

import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.commands.BreadedCommandMap
import dev.theWhiteBread.commands.command.LocationsCommand
import dev.theWhiteBread.commands.command.VeinminerCommand
import dev.theWhiteBread.commands.debug.GiveCommand
import dev.theWhiteBread.commands.debug.MenuCommand
import dev.theWhiteBread.commands.debug.StructuresCommand
import dev.theWhiteBread.items.ItemRegistry
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
import dev.theWhiteBread.storage_system.container.ItemTransport
import kotlinx.coroutines.Job
import org.bukkit.plugin.java.JavaPlugin

class TheWhiteBread : JavaPlugin() {

    override fun onLoad() {
        instance = this
        pluginLogger = PluginLogger(this)
        database = Database().database
    }

    override fun onEnable() {
        // Plugin startup logic
        ItemRegistry.loadItemRegistry()
        PortalManager.load(this)

        ItemTransport.storageManagerPreparation.runTaskTimer(this, 0, 1200L)
        ItemTransport.storageManagerInvExecution.runTaskTimer(this, 60L, 1200L)

        server.scheduler.scheduleSyncRepeatingTask(this, {
            PortalManager.getAllPortals().forEach { portal ->
                portal.renderPortal()
            }
        }, 0, 2)

        Test.register()

        // ------------------ Command Registration ------------------
        VeinminerCommand.register()
        LocationsCommand.register()
        StructuresCommand.register()
        MenuCommand.register()
        GiveCommand.register()

        // Registers grouped commands
        BreadedCommandMap.loadCommands()

        // ------------------ Listener Registration ------------------
        VeinminerListener.register()
        AutoSmeltListener.register()
        EXPBoostListener.register()
        MenuListener.register()
        LoadingOfControllersListener.register()
        ControllerPlacementsListener.register()
        ControllerAccessListener.register()
        ContainerPlacementListeners.register()
        ChatInput.register()
        PortalDetectionListener.register()
        PortalTimingListener.register(); PortalTimingListener.start()
        DimensionalPlacementsListener.register()
        DimensionalAccessListener.register()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        PortalManager.saveAll()
        PortalTimingListener.stop()
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
