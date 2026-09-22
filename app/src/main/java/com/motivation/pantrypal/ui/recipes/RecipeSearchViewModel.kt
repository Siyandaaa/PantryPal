package com.motivation.pantrypal.ui.recipes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto
import com.motivation.pantrypal.data.repository.RecipeRepository
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecipeSearchUiState(
    val isLoading: Boolean = false,
    val results: List<RecipeSummaryDto> = emptyList(),
    val selectedDiet: String? = null,
    val maxMissingIngredients: Int = 3,
    val errorMessage: String? = null
)

class RecipeSearchViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeSearchUiState())
    val uiState: StateFlow<RecipeSearchUiState> = _uiState

    init {
        search()
    }

    fun setDietFilter(diet: String?) {
        _uiState.value = _uiState.value.copy(selectedDiet = diet)
        search()
    }

    fun setMaxMissingIngredients(value: Int) {
        _uiState.value = _uiState.value.copy(maxMissingIngredients = value)
        search()
    }

    fun search() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val state = _uiState.value
            when (val result = repository.findMatchingRecipes(state.selectedDiet, null, state.maxMissingIngredients)) {
                is PantryResult.Success -> _uiState.value = state.copy(isLoading = false, results = result.data)
                is PantryResult.Error -> {
                    Log.w(TAG, "search failed: ${result.message}")
                    _uiState.value = state.copy(isLoading = false, errorMessage = result.message)
                }
                PantryResult.Loading -> Unit
            }
        }
    }

    companion object { private const val TAG = "RecipeSearchViewModel" }
}
