package com.motivation.pantrypal.di

import android.content.Context
import androidx.room.Room
import com.motivation.pantrypal.data.local.AppDatabase
import com.motivation.pantrypal.data.prefs.SessionManager
import com.motivation.pantrypal.data.remote.ApiService
import com.motivation.pantrypal.data.remote.NetworkModule
import com.motivation.pantrypal.data.remote.SpoonacularApiService
import com.motivation.pantrypal.data.repository.*

/**
 * Small hand-rolled dependency container (kept deliberately simple, without
 * Hilt/Dagger, to keep the prototype's build graph easy to reason about).
 * Every dependency is a singleton for the lifetime of the process.
 *
 * Two Retrofit-backed services exist side by side here, reflecting the
 * POE's API scope: [apiService] talks to PantryPal's own custom auth API
 * (the one required backend); [spoonacularApiService] talks to Spoonacular
 * directly for recipe data, which the brief doesn't require a proxy for.
 * Everything else (pantry, meal plan, shopping list, favourites) reads and
 * writes only [database].
 */
class AppContainer(context: Context) {

    val sessionManager: SessionManager = SessionManager(context)

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "pantry_pal.db"
    ).fallbackToDestructiveMigration().build()

    private val apiService: ApiService = NetworkModule.provideApiService(sessionManager)
    private val spoonacularApiService: SpoonacularApiService = NetworkModule.provideSpoonacularApiService()

    val authRepository: AuthRepository by lazy { AuthRepository(apiService, sessionManager) }

    val pantryRepository: PantryRepository by lazy {
        PantryRepository(database.pantryDao(), spoonacularApiService)
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(spoonacularApiService, database.pantryDao())
    }

    val mealPlanRepository: MealPlanRepository by lazy {
        MealPlanRepository(database.mealPlanDao())
    }

    val shoppingListRepository: ShoppingListRepository by lazy {
        ShoppingListRepository(database.shoppingListDao(), database.mealPlanDao(), recipeRepository)
    }

    val favouritesRepository: FavouritesRepository by lazy {
        FavouritesRepository(database.favouriteDao())
    }
}
