package com.motivation.pantrypal.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.local.entity.FavouriteRecipeEntity
import com.motivation.pantrypal.data.repository.FavouritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FavouritesUiState(val recipes: List<FavouriteRecipeEntity> = emptyList())

class FavouritesViewModel(private val repository: FavouritesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState: StateFlow<FavouritesUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeAll().collect { _uiState.value = FavouritesUiState(it) }
        }
    }
}
