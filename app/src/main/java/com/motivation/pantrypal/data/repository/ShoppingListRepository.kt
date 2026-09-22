package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.local.dao.MealPlanDao
import com.motivation.pantrypal.data.local.dao.ShoppingListDao
import com.motivation.pantrypal.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The shopping list is local-only, aggregated on-device from the local meal
 * plan rather than by a server (see [PantryRepository] for why there's no
 * backend involved). "Generate List" walks every planned recipe, fetches
 * its ingredients (via [RecipeRepository]'s Spoonacular cache), scales them
 * by servings, and sums duplicates across recipes - this is a direct port
 * of the aisle-aggregation logic originally designed for the Part 1
 * back-end, just running client-side instead.
 */
class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val mealPlanDao: MealPlanDao,
    private val recipeRepository: RecipeRepository
) {
    companion object { private const val TAG = "ShoppingListRepository" }

    fun observeAll(): Flow<List<ShoppingListItemEntity>> = shoppingListDao.observeAll()

    suspend fun regenerateFromMealPlan() {
        val entries = mealPlanDao.getAllOnce()
        Log.i(TAG, "regenerateFromMealPlan: aggregating ${entries.size} planned recipe(s)")

        val previouslyChecked = shoppingListDao.getAutoItemsOnce()
            .associate { it.ingredientName.lowercase() to it.isChecked }

        data class Aggregate(var quantity: Double, val unit: String?)
        val aggregated = linkedMapOf<String, Aggregate>() // key: "name|unit"

        for (entry in entries) {
            val detail = recipeRepository.getCachedOrFetchDetail(entry.recipeId) ?: continue
            val scale = entry.servingsPlanned.toDouble() / detail.servings.coerceAtLeast(1)
            for (ingredient in detail.ingredients) {
                val key = "${ingredient.name.lowercase()}|${ingredient.unit.orEmpty()}"
                val scaledAmount = ingredient.amount * scale
                val existing = aggregated[key]
                if (existing != null) {
                    existing.quantity += scaledAmount
                } else {
                    aggregated[key] = Aggregate(scaledAmount, ingredient.unit)
                }
            }
        }

        shoppingListDao.clearAutoItems()
        val newItems = aggregated.entries.map { (key, agg) ->
            val name = key.substringBeforeLast('|')
            ShoppingListItemEntity(
                id = "shop-${UUID.randomUUID()}",
                ingredientName = name,
                quantity = Math.round(agg.quantity * 100) / 100.0,
                unit = agg.unit,
                aisle = inferAisle(name),
                isChecked = previouslyChecked[name.lowercase()] ?: false,
                isManuallyAdded = false
            )
        }
        shoppingListDao.upsertAll(newItems)
        Log.i(TAG, "regenerateFromMealPlan: wrote ${newItems.size} aggregated item(s)")
    }

    suspend fun addManualItem(name: String, quantity: Double, unit: String?) {
        shoppingListDao.upsert(
            ShoppingListItemEntity(
                id = "shop-${UUID.randomUUID()}",
                ingredientName = name,
                quantity = quantity,
                unit = unit,
                aisle = inferAisle(name),
                isManuallyAdded = true
            )
        )
    }

    suspend fun setChecked(id: String, checked: Boolean, current: ShoppingListItemEntity) {
        shoppingListDao.upsert(current.copy(isChecked = checked))
    }

    /**
     * Deliberately simple keyword -> aisle mapping so the list groups items
     * the way a real grocery store is laid out, without needing an extra
     * network call per ingredient just to look up its category.
     */
    private fun inferAisle(ingredientName: String): String {
        val lower = ingredientName.lowercase()
        val aisles = listOf(
            "Produce" to listOf("onion", "garlic", "tomato", "pepper", "lettuce", "carrot", "potato", "basil", "lemon", "lime", "herb", "spinach", "apple", "banana"),
            "Meat & Seafood" to listOf("chicken", "beef", "pork", "fish", "salmon", "shrimp", "bacon", "sausage", "mince"),
            "Dairy & Eggs" to listOf("milk", "cheese", "butter", "egg", "yogurt", "cream"),
            "Bakery" to listOf("bread", "flour", "tortilla", "bun", "baguette"),
            "Pantry & Canned Goods" to listOf("rice", "pasta", "bean", "lentil", "stock", "broth", "oil", "vinegar", "sauce", "canned"),
            "Spices & Seasoning" to listOf("salt", "pepper", "cumin", "paprika", "cinnamon", "spice", "seasoning")
        )
        return aisles.firstOrNull { (_, keywords) -> keywords.any { lower.contains(it) } }?.first ?: "Other"
    }
}
