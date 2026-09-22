package com.motivation.pantrypal.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.local.entity.ShoppingListItemEntity
import com.motivation.pantrypal.data.repository.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShoppingListUiState(val items: List<ShoppingListItemEntity> = emptyList())

class ShoppingListViewModel(private val repository: ShoppingListRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeAll().collect { items -> _uiState.value = ShoppingListUiState(items) }
        }
    }

    fun toggleChecked(item: ShoppingListItemEntity) {
        viewModelScope.launch { repository.setChecked(item.id, !item.isChecked, item) }
    }

    fun regenerate() {
        viewModelScope.launch { repository.regenerateFromMealPlan() }
    }

    fun addManualItem(name: String, quantity: Double, unit: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addManualItem(name, quantity, unit) }
    }
}
