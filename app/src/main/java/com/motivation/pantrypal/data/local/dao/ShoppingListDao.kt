package com.motivation.pantrypal.data.local.dao

import androidx.room.*
import com.motivation.pantrypal.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY aisle, ingredientName")
    fun observeAll(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE isManuallyAdded = 0")
    suspend fun getAutoItemsOnce(): List<ShoppingListItemEntity>

    @Query("DELETE FROM shopping_list_items WHERE isManuallyAdded = 0")
    suspend fun clearAutoItems(): Int

    @Upsert
    suspend fun upsertAll(items: List<ShoppingListItemEntity>)

    @Upsert
    suspend fun upsert(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearAll()

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
