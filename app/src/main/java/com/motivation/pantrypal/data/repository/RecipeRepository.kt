package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.local.dao.PantryDao
import com.motivation.pantrypal.data.remote.SpoonacularApiService
import com.motivation.pantrypal.data.remote.dto.NutritionDto
import com.motivation.pantrypal.data.remote.dto.RecipeDetailDto
import com.motivation.pantrypal.data.remote.dto.RecipeIngredientDto
import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.first

/**
 * "What's in My Pantry?" recipe discovery, calling Spoonacular directly
 * (recipe data isn't part of PantryPal's one required custom API - that's
 * authentication only; see root README, "API Scope"). Ranking/filtering
 * that a backend proxy would otherwise do server-side happens here instead.
 * A small in-memory cache avoids re-fetching the same recipe's detail
 * (e.g. once for its detail screen, again when the shopping list
 * aggregates ingredients from the meal plan) within one app session.
 */
class RecipeRepository(
    private val spoonacularApi: SpoonacularApiService,
    private val pantryDao: PantryDao
) {
    companion object { private const val TAG = "RecipeRepository" }

    private var lastResults: List<RecipeSummaryDto> = emptyList()
    private val detailCache = mutableMapOf<Int, RecipeDetailDto>()

    suspend fun findMatchingRecipes(
        diet: String? = null,
        maxPrepTime: Int? = null,
        maxMissingIngredients: Int? = null
    ): PantryResult<List<RecipeSummaryDto>> {
        val ingredientNames = pantryDao.observeAll().first().map { it.name }
        if (ingredientNames.isEmpty()) {
            lastResults = emptyList()
            return PantryResult.Success(emptyList())
        }
        return try {
            Log.i(TAG, "findMatchingRecipes: ${ingredientNames.size} pantry item(s), diet=$diet maxPrepTime=$maxPrepTime maxMissing=$maxMissingIngredients")
            val raw = spoonacularApi.findByIngredients(ingredientNames.joinToString(","))

            var mapped = raw.map {
                RecipeSummaryDto(
                    id = it.id,
                    title = it.title,
                    imageUrl = it.image,
                    readyInMinutes = 0, // findByIngredients doesn't return this; shown once the detail screen loads
                    usedIngredientCount = it.usedIngredientCount,
                    missedIngredientCount = it.missedIngredientCount,
                    totalIngredientCount = it.usedIngredientCount + it.missedIngredientCount
                )
            }

            if (maxMissingIngredients != null) {
                mapped = mapped.filter { it.missedIngredientCount <= maxMissingIngredients }
            }

            if (!diet.isNullOrBlank() && mapped.isNotEmpty()) {
                // Diet info isn't in the findByIngredients response, so a single
                // bulk lookup resolves it for all candidates in one extra call
                // rather than one call per recipe.
                val ids = mapped.joinToString(",") { it.id.toString() }
                val bulkInfo = spoonacularApi.getRecipeInformationBulk(ids)
                bulkInfo.forEach { detailCache[it.id] = mapToDetail(it, emptySet()) }
                val dietMatches = bulkInfo.filter { info -> info.diets.any { it.equals(diet, ignoreCase = true) } }.map { it.id }.toSet()
                mapped = mapped.filter { it.id in dietMatches }
                if (maxPrepTime != null) {
                    mapped = mapped.filter { r -> bulkInfo.find { it.id == r.id }?.readyInMinutes?.let { it <= maxPrepTime } ?: true }
                }
            }

            lastResults = mapped
            PantryResult.Success(mapped)
        } catch (e: Exception) {
            Log.e(TAG, "findMatchingRecipes failed: ${e.message}", e)
            PantryResult.Error(e.message ?: "Could not search for recipes right now")
        }
    }

    /** Cached results, used so returning from the detail screen doesn't refetch. */
    fun cachedResults(): List<RecipeSummaryDto> = lastResults

    suspend fun getRecipeDetail(id: Int): PantryResult<RecipeDetailDto> {
        detailCache[id]?.let { return PantryResult.Success(it) }
        return try {
            val info = spoonacularApi.getRecipeInformation(id, includeNutrition = true)
            val ownedNames = pantryDao.observeAll().first().map { it.name.lowercase() }.toSet()
            val detail = mapToDetail(info, ownedNames)
            detailCache[id] = detail
            PantryResult.Success(detail)
        } catch (e: Exception) {
            Log.e(TAG, "getRecipeDetail($id) failed: ${e.message}", e)
            PantryResult.Error(e.message ?: "Could not load that recipe right now")
        }
    }

    /** Exposes the cache to [ShoppingListRepository] so aggregating ingredients across the meal plan avoids re-fetching. */
    suspend fun getCachedOrFetchDetail(id: Int): RecipeDetailDto? = when (val result = getRecipeDetail(id)) {
        is PantryResult.Success -> result.data
        else -> null
    }

    private fun mapToDetail(
        info: com.motivation.pantrypal.data.remote.dto.SpoonacularRecipeInformation,
        ownedIngredientNamesLowercase: Set<String>
    ): RecipeDetailDto {
        val nutrients = info.nutrition?.nutrients.orEmpty()
        fun nutrientValue(name: String) = nutrients.find { it.name == name }?.amount ?: 0.0
        fun percentDaily(name: String) = nutrients.find { it.name == name }?.percentOfDailyNeeds?.let { Math.round(it).toInt() }

        val percentDailyValues = buildMap {
            listOf("Calories", "Protein", "Carbohydrates", "Fat", "Fiber").forEach { name ->
                percentDaily(name)?.let { put(name, it) }
            }
        }

        return RecipeDetailDto(
            id = info.id,
            title = info.title,
            imageUrl = info.image,
            readyInMinutes = info.readyInMinutes,
            servings = info.servings.coerceAtLeast(1),
            cuisine = info.cuisines.firstOrNull(),
            ingredients = info.extendedIngredients.map {
                RecipeIngredientDto(
                    name = it.name,
                    amount = it.amount,
                    unit = it.unit,
                    ownedByUser = it.name.lowercase() in ownedIngredientNamesLowercase
                )
            },
            steps = info.analyzedInstructions.firstOrNull()?.steps?.map { it.step } ?: emptyList(),
            nutrition = NutritionDto(
                calories = Math.round(nutrientValue("Calories")).toInt(),
                protein = nutrientValue("Protein"),
                carbs = nutrientValue("Carbohydrates"),
                fat = nutrientValue("Fat"),
                fibre = nutrientValue("Fiber"),
                percentDailyValues = percentDailyValues
            )
        )
    }
}
