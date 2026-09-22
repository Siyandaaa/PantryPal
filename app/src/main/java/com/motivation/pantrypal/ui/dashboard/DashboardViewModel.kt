package com.motivation.pantrypal.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.local.entity.PantryItemEntity
import com.motivation.pantrypal.data.prefs.SessionManager
import com.motivation.pantrypal.data.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String? = null,
    val pantryItems: List<PantryItemEntity> = emptyList()
)

class DashboardViewModel(
    private val pantryRepository: PantryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        Log.d(TAG, "init: observing pantry + session for dashboard")
        viewModelScope.launch {
            combine(pantryRepository.observePantry(), sessionManager.userName) { pantry, name ->
                DashboardUiState(userName = name, pantryItems = pantry)
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch { pantryRepository.refreshFromServer() }
    }

    companion object { private const val TAG = "DashboardViewModel" }
}
