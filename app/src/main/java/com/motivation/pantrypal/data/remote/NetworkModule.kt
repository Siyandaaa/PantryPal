package com.motivation.pantrypal.data.remote

import android.util.Log
import com.motivation.pantrypal.BuildConfig
import com.motivation.pantrypal.data.prefs.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Attaches the stored Firebase ID token (if any) as a Bearer token to every
 * request against PantryPal's own auth API, and logs the round-trip so
 * screen-recording the "Web Services" requirement is straightforward from
 * Logcat.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionManager.currentAccessToken() }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        Log.d("ApiRequest", "${request.method} ${request.url}")
        return chain.proceed(request)
    }
}

/** Appends `?apiKey=...` to every outgoing Spoonacular request so it never has to be repeated per-call. */
class SpoonacularKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val urlWithKey: HttpUrl = original.url.newBuilder().addQueryParameter("apiKey", apiKey).build()
        val request = original.newBuilder().url(urlWithKey).build()
        Log.d("SpoonacularRequest", "${request.method} ${request.url}")
        return chain.proceed(request)
    }
}

object NetworkModule {

    fun provideRetrofit(sessionManager: SessionManager): Retrofit {
        val logging = HttpLoggingInterceptor { message -> Log.d("OkHttp", message) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideApiService(sessionManager: SessionManager): ApiService =
        provideRetrofit(sessionManager).create(ApiService::class.java)

    fun provideSpoonacularApiService(): SpoonacularApiService {
        val logging = HttpLoggingInterceptor { message -> Log.d("OkHttp-Spoonacular", message) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(SpoonacularKeyInterceptor(BuildConfig.SPOONACULAR_API_KEY))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularApiService::class.java)
    }
}
