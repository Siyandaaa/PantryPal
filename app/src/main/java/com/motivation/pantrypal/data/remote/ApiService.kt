package com.motivation.pantrypal.data.remote

import com.motivation.pantrypal.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * PantryPal's ONE required custom API: authentication, backed by Firebase.
 *
 * Per the POE brief, a custom REST API connected to a database is only
 * required for login/registration, not for every feature - so this
 * interface deliberately covers auth + the authenticated user's profile
 * and nothing else. Every other feature (pantry, meal plan, shopping list,
 * favourites) is served entirely from the local Room database; recipe
 * search/detail is called directly against Spoonacular via
 * [SpoonacularApiService] instead of being proxied here.
 */
interface ApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    /** body.idToken is the raw Google ID token from GoogleSignInClient's .requestIdToken(webClientId). */
    @POST("api/v1/auth/sso/google")
    suspend fun ssoLoginWithGoogle(@Body body: GoogleSsoRequest): AuthResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): RefreshResponse

    @POST("api/v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): Response<Unit>

    @GET("api/v1/users/me")
    suspend fun getMe(): UserDto

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body body: UpdateProfileRequest): UserDto

    @DELETE("api/v1/users/me")
    suspend fun deleteMe(): Response<Unit>
}
