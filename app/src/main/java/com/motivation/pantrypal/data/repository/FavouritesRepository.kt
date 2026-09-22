package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.local.dao.FavouriteDao
import com.motivation.pantrypal.data.local.entity.FavouriteRecipeEntity
import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Favourites and custom collections are local-only (see [PantryRepository]
 * for why) - saved recipes live entirely in Room on this device.
 */
class FavouritesRepository(private val favouriteDao: FavouriteDao) {
    companion object { private const val TAG = "FavouritesRepository" }

    fun observeAll(): Flow<List<FavouriteRecipeEntity>> = favouriteDao.observeAll()
    fun observeByCollection(collection: String): Flow<List<FavouriteRecipeEntity>> =
        favouriteDao.observeByCollection(collection)

    suspend fun toggleFavourite(recipe: RecipeSummaryDto) {
        val alreadySaved = favouriteDao.isFavourite(recipe.id)
        if (alreadySaved) {
            Log.i(TAG, "toggleFavourite: removing ${recipe.title}")
            favouriteDao.deleteById(recipe.id)
        } else {
            Log.i(TAG, "toggleFavourite: saving ${recipe.title}")
            favouriteDao.upsert(FavouriteRecipeEntity(recipe.id, recipe.title, recipe.imageUrl, recipe.readyInMinutes))
        }
    }

    suspend fun moveToCollection(recipeId: Int, collectionName: String) {
        val current = favouriteDao.observeAll().first().find { it.recipeId == recipeId } ?: return
        Log.i(TAG, "moveToCollection: recipe=$recipeId -> $collectionName")
        favouriteDao.upsert(current.copy(collectionName = collectionName))
    }
}
