package dev.theWhiteBread.listeners.input

import dev.theWhiteBread.Keys
import dev.theWhiteBread.listeners.BreadListener
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object ChatInput : BreadListener {

    private val chatInputHandlers: MutableMap<UUID, (Player, String) -> Unit> = mutableMapOf()

    fun registerNewInputListener(playerUUID: UUID, callbackFunction: (Player, String) -> Unit) {
        Bukkit.getPlayer(playerUUID)!!.persistentDataContainer.set(Keys.getPlayerInput, PersistentDataType.BOOLEAN, true)
        chatInputHandlers[playerUUID] = callbackFunction
    }

    fun finishInputListening(playerUUID: UUID) {
        Bukkit.getPlayer(playerUUID)!!.persistentDataContainer.remove(Keys.getPlayerInput)
        chatInputHandlers.remove(playerUUID)
    }

    @EventHandler
    fun cleanUp(event: PlayerQuitEvent) {
        val player = event.player
        player.persistentDataContainer.remove(Keys.getPlayerInput)
    }

    @EventHandler
    fun getChatInput(event: AsyncChatEvent) {
        val player = event.player
        val message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
            .plainText()
            .serialize(event.message())

        if (!player.persistentDataContainer.has(Keys.getPlayerInput)) return
        if (player.persistentDataContainer.get(Keys.getPlayerInput, PersistentDataType.BOOLEAN)!!) {
            event.isCancelled = true
            chatInputHandlers[player.uniqueId]?.invoke(player, message) ?: return
        }
    }

}