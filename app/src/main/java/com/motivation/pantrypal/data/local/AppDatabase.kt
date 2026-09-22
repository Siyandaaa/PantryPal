package com.motivation.pantrypal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.motivation.pantrypal.data.local.dao.*
import com.motivation.pantrypal.data.local.entity.*

/**
 * Single Room database backing PantryPal. Per the POE brief, the custom
 * REST API is required only for authentication (see AuthRepository); every
 * other feature - pantry, meal planning, the shopping list, favourites -
 * is persisted entirely on-device here, so this database (not a server) is
 * the single source of truth for that data.
 */
@Database(
    entities = [
        PantryItemEntity::class,
        FavouriteRecipeEntity::class,
        MealPlanEntryEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
}
