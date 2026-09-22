package com.motivation.pantrypal.ui.mealplan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.local.entity.MealPlanEntryEntity
import com.motivation.pantrypal.data.repository.MealPlanRepository
import com.motivation.pantrypal.data.repository.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields

data class MealPlanUiState(
    val weekStart: LocalDate = LocalDate.now().with(WeekFields.ISO.dayOfWeek(), 1),
    val entries: List<MealPlanEntryEntity> = emptyList(),
    val isGeneratingList: Boolean = false
)

/** Screen 8: weekly meal-planning calendar (breakfast/lunch/dinner x 7 days). */
class MealPlanViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState())
    val uiState: StateFlow<MealPlanUiState> = _uiState

    init {
        loadWeek(_uiState.value.weekStart)
    }

    fun loadWeek(weekStart: LocalDate) {
        val weekEnd = weekStart.plusDays(6)
        _uiState.value = _uiState.value.copy(weekStart = weekStart)
        viewModelScope.launch {
            mealPlanRepository.refresh(weekStart.toString(), weekEnd.toString())
            mealPlanRepository.observeRange(weekStart.toString(), weekEnd.toString())
                .collect { entries -> _uiState.value = _uiState.value.copy(entries = entries) }
        }
    }

    fun nextWeek() = loadWeek(_uiState.value.weekStart.plusWeeks(1))
    fun previousWeek() = loadWeek(_uiState.value.weekStart.minusWeeks(1))

    fun removeEntry(id: String) {
        viewModelScope.launch { mealPlanRepository.removeEntry(id) }
    }

    /** "Generate List" button - consolidates the week's recipes into an aisle-organised shopping list. */
    fun generateShoppingList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingList = true)
            Log.i(TAG, "generateShoppingList: regenerating from current plan")
            shoppingListRepository.regenerateFromMealPlan()
            _uiState.value = _uiState.value.copy(isGeneratingList = false)
        }
    }

    companion object { private const val TAG = "MealPlanViewModel" }
}
