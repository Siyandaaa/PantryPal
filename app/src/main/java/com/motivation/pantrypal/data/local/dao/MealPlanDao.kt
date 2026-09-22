package com.motivation.pantrypal.data.local.dao

import androidx.room.*
import com.motivation.pantrypal.data.local.entity.MealPlanEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan_entries WHERE planDate BETWEEN :from AND :to ORDER BY planDate, mealSlot")
    fun observeRange(from: String, to: String): Flow<List<MealPlanEntryEntity>>

    /** One-shot read of the whole plan, used to rebuild the shopping list from every planned recipe. */
    @Query("SELECT * FROM meal_plan_entries")
    suspend fun getAllOnce(): List<MealPlanEntryEntity>

    @Upsert
    suspend fun upsertAll(entries: List<MealPlanEntryEntity>)

    @Upsert
    suspend fun upsert(entry: MealPlanEntryEntity)

    @Query("DELETE FROM meal_plan_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
