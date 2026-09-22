package com.motivation.pantrypal.data.local.dao

import androidx.room.*
import com.motivation.pantrypal.data.local.entity.FavouriteRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourite_recipes ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavouriteRecipeEntity>>

    @Query("SELECT * FROM favourite_recipes WHERE collectionName = :collection ORDER BY savedAt DESC")
    fun observeByCollection(collection: String): Flow<List<FavouriteRecipeEntity>>

    @Upsert
    suspend fun upsert(item: FavouriteRecipeEntity)

    @Query("DELETE FROM favourite_recipes WHERE recipeId = :recipeId")
    suspend fun deleteById(recipeId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_recipes WHERE recipeId = :recipeId)")
    suspend fun isFavourite(recipeId: Int): Boolean
}
