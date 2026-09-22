package com.motivation.pantrypal.ui.recipes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.remote.dto.RecipeDetailDto
import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto
import com.motivation.pantrypal.data.repository.FavouritesRepository
import com.motivation.pantrypal.data.repository.MealPlanRepository
import com.motivation.pantrypal.data.repository.RecipeRepository
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: RecipeDetailDto? = null,
    val errorMessage: String? = null,
    val addedToPlanMessage: String? = null
)

class RecipeDetailViewModel(
    private val recipeId: Int,
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val favouritesRepository: FavouritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState

    init {
        Log.d(TAG, "init: loading recipe detail for id=$recipeId")
        viewModelScope.launch {
            when (val result = recipeRepository.getRecipeDetail(recipeId)) {
                is PantryResult.Success -> _uiState.value = RecipeDetailUiState(isLoading = false, recipe = result.data)
                is PantryResult.Error -> _uiState.value = RecipeDetailUiState(isLoading = false, errorMessage = result.message)
                PantryResult.Loading -> Unit
            }
        }
    }

    fun addToPlan(planDate: String, mealSlot: String) {
        val recipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            mealPlanRepository.planRecipe(recipe, planDate, mealSlot)
            _uiState.value = _uiState.value.copy(addedToPlanMessage = "Added to $mealSlot on $planDate")
        }
    }

    fun toggleFavourite() {
        val recipe = _uiState.value.recipe ?: return
        viewModelScope.launch {
            favouritesRepository.toggleFavourite(
                RecipeSummaryDto(
                    id = recipe.id, title = recipe.title, imageUrl = recipe.imageUrl,
                    readyInMinutes = recipe.readyInMinutes, usedIngredientCount = 0,
                    missedIngredientCount = 0, totalIngredientCount = recipe.ingredients.size,
                    cuisine = recipe.cuisine
                )
            )
        }
    }

    companion object { private const val TAG = "RecipeDetailViewModel" }
}
