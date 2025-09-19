package dev.theWhiteBread.input

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player

class PaginatedChatMenu<T>(
    private val id: String,
    private val items: List<T>,
    private val pageSize: Int = 6,
    private val itemRenderer: (T, Int) -> Component // (item, indexOnPage) -> Component
) {
    private val lastPageIndex: Int
        get() = (items.size - 1) / pageSize

    fun open(player: Player, page: Int = 0) {
        val p = page.coerceIn(0, lastPageIndex)
        val from = p * pageSize
        val to = ((p + 1) * pageSize).coerceAtMost(items.size)

        player.sendMessage(Component.text("------ Menu (Page ${p + 1}/${lastPageIndex + 1}) ------",
            NamedTextColor.GRAY))

        for (i in from until to) {
            val idxOnPage = i - from
            val line = itemRenderer(items[i], idxOnPage)
            player.sendMessage(line)
        }

        // navigation bar
        val prev = if (p > 0) {
            Component.text("◀ Prev", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.callback { run {
                    open(player, p - 1)
                }})
                .hoverEvent(HoverEvent.showText(Component.text("Go to previous page")))
        } else {
            Component.text("◀ Prev", NamedTextColor.DARK_GRAY)
        }

        val next = if (p < lastPageIndex) {
            Component.text("Next ▶", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.callback { run {
                    open(player, p + 1)
                }})                .hoverEvent(HoverEvent.showText(Component.text("Go to next page")))
        } else {
            Component.text("Next ▶", NamedTextColor.DARK_GRAY)
        }

        val nav = Component.text()
            .append(prev)
            .append(Component.text("  ", NamedTextColor.GRAY))
            .append(next)
            .build()

        player.sendMessage(nav)
    }
}