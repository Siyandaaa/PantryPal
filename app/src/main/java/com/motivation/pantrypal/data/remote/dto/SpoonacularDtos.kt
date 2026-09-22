package com.motivation.pantrypal.data.remote.dto

/**
 * These mirror Spoonacular's own field names/shapes (see
 * https://spoonacular.com/food-api/docs) exactly, since the Android app
 * calls Spoonacular directly - there's no PantryPal backend re-shaping the
 * JSON in between for anything other than authentication. [RecipeRepository]
 * maps these into the app's own [RecipeSummaryDto] / [RecipeDetailDto]
 * domain models used throughout the UI.
 */

data class SpoonacularFindByIngredientsItem(
    val id: Int,
    val title: String,
    val image: String?,
    val usedIngredientCount: Int,
    val missedIngredientCount: Int
)

data class SpoonacularRecipeInformation(
    val id: Int,
    val title: String,
    val image: String?,
    val readyInMinutes: Int,
    val servings: Int,
    val cuisines: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
    val extendedIngredients: List<SpoonacularIngredient> = emptyList(),
    val analyzedInstructions: List<SpoonacularInstructionGroup> = emptyList(),
    val nutrition: SpoonacularNutrition? = null
)

data class SpoonacularIngredient(
    val name: String,
    val amount: Double,
    val unit: String?
)

data class SpoonacularInstructionGroup(val steps: List<SpoonacularStep> = emptyList())
data class SpoonacularStep(val number: Int, val step: String)

data class SpoonacularNutrition(val nutrients: List<SpoonacularNutrient> = emptyList())
data class SpoonacularNutrient(val name: String, val amount: Double, val unit: String, val percentOfDailyNeeds: Double = 0.0)

data class SpoonacularAutocompleteItem(val name: String)

data class SpoonacularParsedIngredient(val name: String, val amount: Double = 1.0, val unit: String? = null)
