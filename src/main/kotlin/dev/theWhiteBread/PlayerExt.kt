package dev.theWhiteBread

import org.bukkit.entity.Player


fun Player.message(msg: String) =
    this.sendMessage(miniMessage.deserialize(msg))
