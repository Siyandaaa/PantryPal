package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.local.dao.MealPlanDao
import com.motivation.pantrypal.data.local.entity.MealPlanEntryEntity
import com.motivation.pantrypal.data.remote.dto.RecipeDetailDto
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * The weekly meal plan is local-only, same as the pantry (see
 * [PantryRepository] for why) - Room is the single source of truth, so
 * there's nothing to pull from a server here.
 */
class MealPlanRepository(private val mealPlanDao: MealPlanDao) {
    companion object { private const val TAG = "MealPlanRepository" }

    fun observeRange(from: String, to: String): Flow<List<MealPlanEntryEntity>> =
        mealPlanDao.observeRange(from, to)

    /** Kept for API symmetry with screens that call it on launch; there's no remote copy to pull. */
    suspend fun refresh(from: String, to: String) {
        Log.d(TAG, "refresh: meal plan is local-only, nothing to pull for $from..$to")
    }

    suspend fun planRecipe(recipe: RecipeDetailDto, planDate: String, mealSlot: String) {
        Log.i(TAG, "planRecipe: ${recipe.title} -> $planDate/$mealSlot")
        mealPlanDao.upsert(
            MealPlanEntryEntity(
                id = "plan-${UUID.randomUUID()}",
                recipeId = recipe.id,
                recipeTitle = recipe.title,
                recipeImageUrl = recipe.imageUrl,
                planDate = planDate,
                mealSlot = mealSlot,
                servingsPlanned = recipe.servings
            )
        )
    }

    suspend fun moveEntry(id: String, newDate: String, newSlot: String) {
        val existing = mealPlanDao.getAllOnce().find { it.id == id } ?: return
        Log.i(TAG, "moveEntry: $id -> $newDate/$newSlot")
        mealPlanDao.upsert(existing.copy(planDate = newDate, mealSlot = newSlot))
    }

    suspend fun removeEntry(id: String) {
        Log.i(TAG, "removeEntry: $id")
        mealPlanDao.deleteById(id)
    }
}
