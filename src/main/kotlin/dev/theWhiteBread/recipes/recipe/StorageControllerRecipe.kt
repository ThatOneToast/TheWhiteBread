package dev.theWhiteBread.recipes.recipe

import dev.theWhiteBread.Keys
import dev.theWhiteBread.items.ItemRegistry
import dev.theWhiteBread.items.item.StorageManagerItem
import dev.theWhiteBread.recipes.Recipe
import dev.theWhiteBread.storage_manager.StorageRegistry
import org.bukkit.Material
import org.bukkit.inventory.ShapedRecipe

object StorageControllerRecipe : Recipe {
    override fun recipe(): ShapedRecipe {
        return ShapedRecipe(
            Keys.storageControllerRecipe,
            StorageManagerItem.item
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