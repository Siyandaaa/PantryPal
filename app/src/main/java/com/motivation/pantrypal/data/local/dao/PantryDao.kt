package com.motivation.pantrypal.data.local.dao

import androidx.room.*
import com.motivation.pantrypal.data.local.entity.PantryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY category, name")
    fun observeAll(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE name LIKE '%' || :search || '%'")
    suspend fun search(search: String): List<PantryItemEntity>

    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PantryItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<PantryItemEntity>)

    @Upsert
    suspend fun upsert(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pantry_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pantry_items")
    suspend fun count(): Int
}
