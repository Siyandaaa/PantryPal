package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.prefs.SessionManager
import com.motivation.pantrypal.data.remote.ApiService
import com.motivation.pantrypal.data.remote.dto.*
import com.motivation.pantrypal.util.PantryResult
import com.motivation.pantrypal.util.safeApiCall

/**
 * Wraps every Auth-service endpoint (email/password + SSO) and is the only
 * place that writes tokens into [SessionManager], keeping the "single
 * source of truth for session state" guarantee.
 */
class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {
    companion object { private const val TAG = "AuthRepository" }

    suspend fun register(fullName: String, email: String, password: String): PantryResult<UserDto> =
        safeApiCall(TAG) {
            Log.i(TAG, "register: creating account for $email")
            val response = api.register(RegisterRequest(fullName, email, password))
            persistSession(response)
            response.user
        }

    suspend fun login(email: String, password: String): PantryResult<UserDto> =
        safeApiCall(TAG) {
            Log.i(TAG, "login: authenticating $email")
            val response = api.login(LoginRequest(email, password))
            persistSession(response)
            response.user
        }

    /**
     * Google SSO: the UI obtains a Google ID token on-device via
     * GoogleSignInClient's .requestIdToken(webClientId); the backend
     * exchanges it for a Firebase session via the Identity Toolkit REST API.
     */
    suspend fun ssoLoginWithGoogle(googleIdToken: String): PantryResult<UserDto> =
        safeApiCall(TAG) {
            Log.i(TAG, "ssoLoginWithGoogle: exchanging Google ID token for a session")
            val response = api.ssoLoginWithGoogle(GoogleSsoRequest(googleIdToken))
            persistSession(response)
            response.user
        }

    suspend fun requestPasswordReset(email: String): PantryResult<Unit> =
        safeApiCall(TAG) { api.requestPasswordReset(PasswordResetRequest(email)); Unit }

    /** Firebase ID tokens are short-lived (1h); call this on a 401 before forcing a full re-login. */
    suspend fun refreshSession(): PantryResult<Unit> = safeApiCall(TAG) {
        val refreshToken = sessionManager.currentRefreshToken()
            ?: throw IllegalStateException("No refresh token available")
        val response = api.refresh(RefreshRequest(refreshToken))
        sessionManager.updateTokens(response.idToken, response.refreshToken)
    }

    /** Pulls the latest profile (dietary prefs, allergies, theme) from the server for the Settings screen. */
    suspend fun getProfile(): PantryResult<UserDto> = safeApiCall(TAG) {
        Log.i(TAG, "getProfile: fetching /users/me")
        api.getMe()
    }

    /** Persists profile / preference changes both remotely and, for theme, locally so it applies instantly offline. */
    suspend fun updateProfile(
        dietaryPreferences: List<String>? = null,
        allergies: List<String>? = null,
        theme: String? = null
    ): PantryResult<UserDto> = safeApiCall(TAG) {
        Log.i(TAG, "updateProfile: diet=$dietaryPreferences allergies=$allergies theme=$theme")
        api.updateMe(UpdateProfileRequest(dietaryPreferences = dietaryPreferences, allergies = allergies, theme = theme))
    }

    /** "Sync Now": round-trips the profile so the demo can show a live, verifiable cloud read. */
    suspend fun syncNow(): PantryResult<UserDto> = getProfile()

    suspend fun logout() {
        Log.i(TAG, "logout: clearing local session")
        sessionManager.clearSession()
    }

    private suspend fun persistSession(response: AuthResponse) {
        sessionManager.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
            userName = response.user.fullName
        )
    }
}
