package com.motivation.pantrypal.data.repository

import android.util.Log
import com.motivation.pantrypal.data.local.dao.PantryDao
import com.motivation.pantrypal.data.local.entity.PantryItemEntity
import com.motivation.pantrypal.data.remote.SpoonacularApiService
import com.motivation.pantrypal.data.remote.dto.NewPantryItem
import com.motivation.pantrypal.util.PantryResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Pantry management is entirely local: per the POE brief, PantryPal's one
 * required custom API is authentication only, so "My Pantry" is simply
 * on-device storage in Room - there is no server copy to sync with, which
 * incidentally makes it fully usable offline for free. Ingredient
 * autocomplete and voice-transcript parsing call Spoonacular directly
 * (best-effort - the pantry itself still works if that call fails).
 */
class PantryRepository(
    private val pantryDao: PantryDao,
    private val spoonacularApi: SpoonacularApiService
) {
    companion object { private const val TAG = "PantryRepository" }

    fun observePantry(): Flow<List<PantryItemEntity>> = pantryDao.observeAll()

    /** Kept for API symmetry with screens that call it on launch; there's no remote copy to pull. */
    suspend fun refreshFromServer(): PantryResult<Unit> = PantryResult.Success(Unit)

    suspend fun addIngredients(items: List<NewPantryItem>) {
        Log.i(TAG, "addIngredients: saving ${items.size} item(s) locally")
        pantryDao.upsertAll(
            items.map {
                PantryItemEntity(
                    id = "pantry-${UUID.randomUUID()}",
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    category = it.category
                )
            }
        )
    }

    suspend fun updateQuantity(id: String, quantity: Double, unit: String?) {
        val current = pantryDao.getById(id) ?: return
        pantryDao.upsert(current.copy(quantity = quantity, unit = unit, updatedAt = System.currentTimeMillis()))
    }

    suspend fun removeIngredient(id: String) {
        Log.i(TAG, "removeIngredient: $id")
        pantryDao.deleteById(id)
    }

    /** Ingredient-name suggestions as the user types, straight from Spoonacular's food database. */
    suspend fun autocomplete(query: String): List<String> = try {
        spoonacularApi.autocompleteIngredient(query).map { it.name }
    } catch (e: Exception) {
        Log.w(TAG, "autocomplete: Spoonacular unreachable, returning empty suggestions (${e.message})")
        emptyList()
    }

    /**
     * Voice-Activated Pantry Entry: sends the raw SpeechRecognizer transcript
     * to Spoonacular's ingredient parser. Falls back to a naive comma/"and"
     * split if Spoonacular is briefly unreachable, so the feature still
     * works (just less precisely) offline.
     */
    suspend fun parseVoiceTranscript(transcript: String): PantryResult<List<NewPantryItem>> = try {
        Log.i(TAG, "parseVoiceTranscript: \"$transcript\"")
        val parsed = spoonacularApi.parseIngredients(transcript)
        PantryResult.Success(parsed.map { NewPantryItem(it.name, it.amount, it.unit, category = "Uncategorised") })
    } catch (e: Exception) {
        Log.w(TAG, "parseVoiceTranscript: Spoonacular unreachable, falling back to naive split (${e.message})")
        val fallback = transcript.split(",", " and ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { NewPantryItem(it, 1.0, null, category = "Uncategorised") }
        PantryResult.Success(fallback)
    }
}
