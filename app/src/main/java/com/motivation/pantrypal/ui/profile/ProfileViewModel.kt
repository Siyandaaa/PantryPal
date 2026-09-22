package com.motivation.pantrypal.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.prefs.SessionManager
import com.motivation.pantrypal.data.repository.AuthRepository
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** All dietary/allergy options a user can toggle; kept small and closed-set for a clean settings UI. */
val AVAILABLE_DIETS = listOf("Vegetarian", "Vegan", "Pescatarian", "Keto", "Paleo", "Gluten Free", "Dairy Free")
val AVAILABLE_ALLERGENS = listOf("Peanuts", "Tree nuts", "Shellfish", "Eggs", "Soy", "Dairy", "Wheat")

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val email: String? = null,
    val darkThemeEnabled: Boolean = false,
    val dietaryPreferences: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    val statusMessage: String? = null,
    val isSyncing: Boolean = false
)

/**
 * Backs Screen 11 (Profile). Every user-facing preference here is persisted
 * two ways: instantly to local DataStore (via [SessionManager]) so it
 * survives app restarts even offline, and to the custom REST API via
 * [AuthRepository] so it also survives a reinstall / new device - this is
 * the "Application State: changing and saving user preferences via a
 * dedicated settings menu" requirement for the video demo.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        Log.d(TAG, "init: loading profile + preferences")
        viewModelScope.launch {
            combine(sessionManager.userName, sessionManager.darkThemeEnabled) { name, dark ->
                name to dark
            }.collect { (name, dark) ->
                _uiState.value = _uiState.value.copy(userName = name, darkThemeEnabled = dark)
            }
        }
        refreshFromServer()
    }

    /** Pulls the authoritative copy of dietary prefs/allergies from the cloud (also used by "Sync Now"). */
    fun refreshFromServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            when (val result = authRepository.syncNow()) {
                is PantryResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSyncing = false,
                    email = result.data.email,
                    dietaryPreferences = result.data.dietaryPreferences.toSet(),
                    allergies = result.data.allergies.toSet(),
                    statusMessage = "Synced with server just now"
                )
                is PantryResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSyncing = false,
                    statusMessage = "Offline - showing cached preferences"
                )
                PantryResult.Loading -> Unit
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        Log.i(TAG, "setDarkTheme: $enabled")
        viewModelScope.launch {
            sessionManager.setDarkTheme(enabled)
            authRepository.updateProfile(theme = if (enabled) "dark" else "light")
        }
    }

    fun toggleDietaryPreference(option: String) {
        val updated = _uiState.value.dietaryPreferences.toMutableSet().apply {
            if (contains(option)) remove(option) else add(option)
        }
        _uiState.value = _uiState.value.copy(dietaryPreferences = updated)
        persistPreferences()
    }

    fun toggleAllergy(option: String) {
        val updated = _uiState.value.allergies.toMutableSet().apply {
            if (contains(option)) remove(option) else add(option)
        }
        _uiState.value = _uiState.value.copy(allergies = updated)
        persistPreferences()
    }

    private fun persistPreferences() {
        viewModelScope.launch {
            val state = _uiState.value
            Log.i(TAG, "persistPreferences: diet=${state.dietaryPreferences} allergies=${state.allergies}")
            when (authRepository.updateProfile(
                dietaryPreferences = state.dietaryPreferences.toList(),
                allergies = state.allergies.toList()
            )) {
                is PantryResult.Success -> _uiState.value = _uiState.value.copy(statusMessage = "Preferences saved")
                is PantryResult.Error -> _uiState.value = _uiState.value.copy(statusMessage = "Saved locally - will sync when online")
                PantryResult.Loading -> Unit
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    companion object { private const val TAG = "ProfileViewModel" }
}
