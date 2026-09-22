package com.motivation.pantrypal.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pantry_pal_session")

/**
 * Persists the JWT pair and lightweight user-facing settings (theme, whether
 * onboarding has been seen) locally, so the "Persist user session state
 * across application restarts" requirement holds even fully offline.
 */
class SessionManager(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val userNameKey = stringPreferencesKey("user_name")
    private val darkThemeKey = booleanPreferencesKey("dark_theme_enabled")
    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")

    val accessToken: Flow<String?> = context.dataStore.data.map { it[accessTokenKey] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { !it[accessTokenKey].isNullOrBlank() }
    val darkThemeEnabled: Flow<Boolean> = context.dataStore.data.map { it[darkThemeKey] ?: false }
    val onboardingSeen: Flow<Boolean> = context.dataStore.data.map { it[onboardingSeenKey] ?: false }
    val userName: Flow<String?> = context.dataStore.data.map { it[userNameKey] }

    suspend fun currentAccessToken(): String? = context.dataStore.data.first()[accessTokenKey]
    suspend fun currentRefreshToken(): String? = context.dataStore.data.first()[refreshTokenKey]

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String, userName: String) {
        Log.i(TAG, "saveSession: persisting session for user=$userId")
        context.dataStore.edit {
            it[accessTokenKey] = accessToken
            it[refreshTokenKey] = refreshToken
            it[userIdKey] = userId
            it[userNameKey] = userName
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        context.dataStore.edit { it[accessTokenKey] = accessToken }
    }

    /** Firebase ID tokens expire after 1h; refreshing rotates both the ID token and its refresh token. */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[accessTokenKey] = accessToken
            it[refreshTokenKey] = refreshToken
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[onboardingSeenKey] = true }
    }

    suspend fun clearSession() {
        Log.i(TAG, "clearSession: logging out, clearing tokens")
        context.dataStore.edit {
            it.remove(accessTokenKey)
            it.remove(refreshTokenKey)
            it.remove(userIdKey)
            it.remove(userNameKey)
        }
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
