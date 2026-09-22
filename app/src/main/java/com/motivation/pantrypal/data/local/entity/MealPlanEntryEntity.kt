package com.motivation.pantrypal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plan_entries")
data class MealPlanEntryEntity(
    @PrimaryKey val id: String,
    val recipeId: Int,
    val recipeTitle: String,
    val recipeImageUrl: String?,
    val planDate: String, // ISO yyyy-MM-dd
    val mealSlot: String, // BREAKFAST | LUNCH | DINNER
    val servingsPlanned: Int
)
