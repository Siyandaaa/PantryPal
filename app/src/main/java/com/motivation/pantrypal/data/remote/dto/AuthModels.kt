package com.motivation.pantrypal.data.remote.dto

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

/** idToken is the raw Google ID token obtained on-device via GoogleSignInClient's .requestIdToken(webClientId). */
data class GoogleSsoRequest(val idToken: String)

data class RefreshRequest(val refreshToken: String)

data class PasswordResetRequest(val email: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class RefreshResponse(val idToken: String, val refreshToken: String)

data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,
    val theme: String = "system",
    val dietaryPreferences: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val nutritionGoals: NutritionGoalsDto? = null
)

data class NutritionGoalsDto(
    val calories: Int? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null
)

data class UpdateProfileRequest(
    val theme: String? = null,
    val dietaryPreferences: List<String>? = null,
    val allergies: List<String>? = null,
    val nutritionGoals: NutritionGoalsDto? = null
)
