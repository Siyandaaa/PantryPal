package com.motivation.pantrypal.data.remote.dto

/**
 * PantryPal's own internal domain models, used throughout the UI layer.
 * Unlike [SpoonacularDtos] (raw external API shapes) or [AuthModels] (the
 * one real network contract with PantryPal's own backend), these aren't
 * sent over the wire anywhere - they're just plain data classes that
 * [RecipeRepository] maps Spoonacular's responses into, and that
 * [PantryRepository] uses to shuttle a new pantry entry from the UI down
 * to a Room insert.
 */

data class NewPantryItem(
    val name: String,
    val quantity: Double,
    val unit: String?,
    val category: String
)

data class RecipeSummaryDto(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val readyInMinutes: Int,
    val usedIngredientCount: Int,
    val missedIngredientCount: Int,
    val totalIngredientCount: Int,
    val cuisine: String? = null
) {
    val matchPercent: Int
        get() = if (totalIngredientCount == 0) 0
        else ((usedIngredientCount.toDouble() / totalIngredientCount) * 100).toInt()
}

data class RecipeDetailDto(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val readyInMinutes: Int,
    val servings: Int,
    val cuisine: String?,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<String>,
    val nutrition: NutritionDto
)

data class RecipeIngredientDto(
    val name: String,
    val amount: Double,
    val unit: String?,
    val ownedByUser: Boolean
)

data class NutritionDto(
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fibre: Double,
    val percentDailyValues: Map<String, Int> = emptyMap()
)
