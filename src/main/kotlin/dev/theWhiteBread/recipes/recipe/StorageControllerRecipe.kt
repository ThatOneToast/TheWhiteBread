package dev.theWhiteBread.recipes.recipe

import dev.theWhiteBread.Keys
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.recipes.Recipe
import org.bukkit.Material
import org.bukkit.inventory.ShapedRecipe

object StorageControllerRecipe : Recipe {
    override fun recipe(): ShapedRecipe {
        return ShapedRecipe(
            Keys.storageControllerRecipe,
            ItemRegistry.storageController
        ).apply {
            shape(
                "ICI",
                "BLB",
                "ICI"
            )
            setIngredient('I', Material.IRON_BLOCK)
            setIngredient('C', Material.CHEST)
            setIngredient('B', Material.BARREL)
            setIngredient('L', Material.LECTERN)
        }
    }
}