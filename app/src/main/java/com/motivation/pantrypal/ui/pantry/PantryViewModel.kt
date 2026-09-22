package com.motivation.pantrypal.ui.pantry

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.local.entity.PantryItemEntity
import com.motivation.pantrypal.data.remote.dto.NewPantryItem
import com.motivation.pantrypal.data.repository.PantryRepository
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PantryUiState(
    val items: List<PantryItemEntity> = emptyList(),
    val autocompleteSuggestions: List<String> = emptyList(),
    val isListening: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Backs the "My Pantry" screen (Screen 5). Handles manual add with
 * autocomplete, voice-parsed batch add, quantity edits and removal - all
 * offline-first via [PantryRepository].
 */
class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState

    init {
        Log.d(TAG, "init: PantryViewModel created, observing local pantry")
        viewModelScope.launch {
            repository.observePantry().collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
        viewModelScope.launch { repository.refreshFromServer() }
    }

    fun onSearchQueryChanged(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(autocompleteSuggestions = emptyList())
            return
        }
        viewModelScope.launch {
            val suggestions = repository.autocomplete(query)
            _uiState.value = _uiState.value.copy(autocompleteSuggestions = suggestions)
        }
    }

    fun addIngredientManually(name: String, quantity: Double, unit: String?, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addIngredients(listOf(NewPantryItem(name, quantity, unit, category)))
        }
    }

    /** Called with the raw transcript from Android's SpeechRecognizer. */
    fun onVoiceTranscript(transcript: String) {
        Log.i(TAG, "onVoiceTranscript: '$transcript'")
        _uiState.value = _uiState.value.copy(isListening = false)
        viewModelScope.launch {
            when (val result = repository.parseVoiceTranscript(transcript)) {
                is PantryResult.Success -> repository.addIngredients(result.data)
                is PantryResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                PantryResult.Loading -> Unit
            }
        }
    }

    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }

    fun removeIngredient(id: String) {
        viewModelScope.launch { repository.removeIngredient(id) }
    }

    fun updateQuantity(id: String, quantity: Double, unit: String?) {
        viewModelScope.launch { repository.updateQuantity(id, quantity, unit) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object { private const val TAG = "PantryViewModel" }
}
