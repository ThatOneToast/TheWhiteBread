package dev.theWhiteBread.recipes

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin

val recipes: MutableCollection<Recipe> = mutableSetOf()

interface Recipe {
    fun recipe(): ShapedRecipe


    fun computeRecipe(player: Player): Boolean {
        return if (player.hasDiscoveredRecipe(recipe().key)) true
        else (player.discoverRecipe(recipe().key))
    }

    fun computeRecipe(plugin: JavaPlugin): Boolean {
        recipes.add(this)
        return plugin.server.addRecipe(recipe())
    }

}
