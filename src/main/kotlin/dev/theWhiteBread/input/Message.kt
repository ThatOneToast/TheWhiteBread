package dev.theWhiteBread.input

import dev.theWhiteBread.miniMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object Message {

    /**
     * Parse a MiniMessage template. `placeholders` allows you to inject Components by name.
     * Example template: "<hello>Click me</hello>"
     * placeholders["hello"] = component -> resolves <hello></hello> to that component
     */
    fun parse(template: String, placeholders: Map<String, Component> = emptyMap()): Component {
        if (placeholders.isEmpty()) return miniMessage.deserialize(template)

        val resolvers = placeholders.map { (name, comp) ->
            TagResolver.resolver(name, Tag.inserting(comp))
        }.toTypedArray()

        return miniMessage.deserialize(template, *resolvers)
    }

    /**
     * Convenience overload to pass simple text placeholders (auto-wrapped as Component.text)
     */
    fun parseTextPlaceholders(template: String, textPlaceholders: Map<String, String>): Component {
        val resolvers = textPlaceholders.map { (k, v) ->
            TagResolver.resolver(k, Tag.inserting(Component.text(v)))
        }.toTypedArray()
        return miniMessage.deserialize(template, *resolvers)
    }
}