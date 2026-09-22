package com.motivation.pantrypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_recipes")
data class FavouriteRecipeEntity(
    @PrimaryKey val recipeId: Int,
    val title: String,
    val imageUrl: String?,
    val readyInMinutes: Int,
    val collectionName: String = "Uncategorised",
    val savedAt: Long = System.currentTimeMillis()
)
