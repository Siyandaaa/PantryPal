package com.motivation.pantrypal.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motivation.pantrypal.data.repository.AuthRepository
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords do not match")
            return
        }
        if (fullName.isBlank() || email.isBlank() || password.length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields (password: 8+ characters)")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.register(fullName, email, password)) {
                is PantryResult.Success -> {
                    Log.i(TAG, "register success for ${result.data.email}")
                    _uiState.value = AuthUiState(isAuthenticated = true)
                }
                is PantryResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                PantryResult.Loading -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.login(email, password)) {
                is PantryResult.Success -> _uiState.value = AuthUiState(isAuthenticated = true)
                is PantryResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                PantryResult.Loading -> Unit
            }
        }
    }

    /** Called once GoogleSignInClient returns a signed-in account with a Google ID token attached. */
    fun ssoLoginWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.ssoLoginWithGoogle(googleIdToken)) {
                is PantryResult.Success -> _uiState.value = AuthUiState(isAuthenticated = true)
                is PantryResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                PantryResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object { private const val TAG = "AuthViewModel" }
}
