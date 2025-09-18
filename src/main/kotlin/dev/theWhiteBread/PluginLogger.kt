package dev.theWhiteBread

import org.bukkit.plugin.java.JavaPlugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

class PluginLogger(private val plugin: JavaPlugin) {
    private val logger = plugin.logger
    private val tsFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun nowTs(): String = tsFmt.format(LocalDateTime.now())

    private fun formatLine(tag: String, message: String): String =
        "${nowTs()} [$tag] $message"

    private fun formatMsg(msg: String, vararg args: Any?): String {
        if (args.isEmpty()) return msg
        return try {
            String.format(msg, *args)
        } catch (ex: Exception) {
            "$msg (format failed)"
        }
    }

    fun info(msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        logger.info(formatLine("INFO", m))
    }

    fun warning(msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        logger.warning(formatLine("WARNING", m))
    }

    fun error(msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        logger.severe(formatLine("ERROR", m))
    }

    fun error(t: Throwable, msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        val line = formatLine("ERROR", m)
        logger.log(Level.SEVERE, line, t)
    }

    fun fix(msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        logger.info(formatLine("FIX", m))
    }

    fun unfixable(msg: String, vararg args: Any?) {
        val m = formatMsg(msg, *args)
        logger.warning(formatLine("UNFIXABLE", m))
    }
}