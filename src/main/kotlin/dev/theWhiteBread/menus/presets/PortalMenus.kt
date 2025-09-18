package dev.theWhiteBread.menus.presets

import dev.theWhiteBread.menus.Menu
import dev.theWhiteBread.menus.MenuItem
import dev.theWhiteBread.menus.createMenuReadyItem
import dev.theWhiteBread.portals.DimensionalReceiver
import dev.theWhiteBread.portals.PortalManager
import dev.theWhiteBread.portals.BreachPortal
import dev.theWhiteBread.portals.UnstableBreachPortal
import org.bukkit.Material
import org.bukkit.entity.Player

object PortalMenus {
    val dimensialReceiverOptions: (Player, DimensionalReceiver) -> Menu = { player, receiver ->
        Menu(player,"Dimensial Receiver", rows = 2, traversal = false, displayUtilities = false).apply {
            setItems(listOf(
                MenuItem(createMenuReadyItem("<rainbow>Select Your Color!", emptyList(), Material.CYAN_DYE)) {
                    run {
                        ColorMenu.open(player) { color ->
                            run {
                                receiver.color = color.toString()

                                if (receiver.isUnstable) {
                                    val portal =
                                        PortalManager.getPortalById(receiver.id) as? UnstableBreachPortal
                                            ?: throw IllegalStateException("Cast failed to a UnstableBreach but receiver says we are unstable.")
                                    portal.color = color.color.hashCode()
                                    portal.save()
                                } else {
                                    val portal = PortalManager.getPortalById(receiver.id) as? BreachPortal
                                        ?: throw IllegalStateException("Receiver says we are stable but faileed to cast  to Breach")
                                    portal.color = color.color.hashCode()
                                    portal.save()
                                }

                                player.closeInventory()
                                receiver.save()

                            }
                        }
                    }
                },
                MenuItem(
                    createMenuReadyItem(
                        "<black>test", emptyList(), Material.DIAMOND
                    )) { run {
                    val portal = PortalManager.getPortalById(receiver.id) ?: return@run

                    if (portal is UnstableBreachPortal) {
                        receiver.isUnstable = false
                        receiver.save()
                        portal.toBreachPortal() ?: return@run

                    } else if (portal is BreachPortal) {
                        receiver.isUnstable = true
                        receiver.save()
                        portal.makeUnstable()
                    }
                }}
            ))
        }
    }



}