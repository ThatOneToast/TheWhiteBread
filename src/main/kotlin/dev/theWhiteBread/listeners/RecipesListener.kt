package dev.theWhiteBread.listeners

import dev.theWhiteBread.message
import dev.theWhiteBread.recipes.recipes
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent

object RecipesListener : BreadListener {

    @EventHandler
    fun grantRecipes(event: PlayerJoinEvent) {
        recipes.forEach {
            if (!it.computeRecipe(event.player)) {
                event.player.message("<gray>You were granted the <gold>${it.recipe().key.toString().replace("thewhitebread:", "")}</gold> Recipe")
            }
        }
    }
}