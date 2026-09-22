package com.motivation.pantrypal.repository

import com.motivation.pantrypal.data.local.dao.PantryDao
import com.motivation.pantrypal.data.local.entity.PantryItemEntity
import com.motivation.pantrypal.data.remote.SpoonacularApiService
import com.motivation.pantrypal.data.remote.dto.NewPantryItem
import com.motivation.pantrypal.data.remote.dto.SpoonacularAutocompleteItem
import com.motivation.pantrypal.data.remote.dto.SpoonacularParsedIngredient
import com.motivation.pantrypal.data.repository.PantryRepository
import com.motivation.pantrypal.util.PantryResult
import com.motivation.pantrypal.util.mockAndroidLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The pantry is local-only (see PantryRepository's doc comment for why), so
 * these tests focus on: writes actually landing in Room, and the two
 * Spoonacular-backed conveniences (autocomplete, voice-transcript parsing)
 * degrading gracefully instead of crashing when the network is unavailable.
 */
class PantryRepositoryTest {

    private val pantryDao: PantryDao = mockk(relaxed = true)
    private val spoonacularApi: SpoonacularApiService = mockk()
    private lateinit var repository: PantryRepository

    @Before
    fun setUp() {
        mockAndroidLog()
        repository = PantryRepository(pantryDao, spoonacularApi)
    }

    @Test
    fun `addIngredients writes every item straight to Room`() = runTest {
        val items = listOf(
            NewPantryItem(name = "Tomato", quantity = 3.0, unit = "each", category = "Produce"),
            NewPantryItem(name = "Onion", quantity = 2.0, unit = "each", category = "Produce")
        )

        repository.addIngredients(items)

        coVerify(exactly = 1) {
            pantryDao.upsertAll(match { entities -> entities.size == 2 && entities.all { it.category == "Produce" } })
        }
    }

    @Test
    fun `removeIngredient deletes by id from Room`() = runTest {
        repository.removeIngredient("item-1")
        coVerify(exactly = 1) { pantryDao.deleteById("item-1") }
    }

    @Test
    fun `updateQuantity updates the existing row when found`() = runTest {
        val existing = PantryItemEntity(id = "item-1", name = "Garlic", quantity = 1.0, unit = "bulb", category = "Produce")
        coEvery { pantryDao.getById("item-1") } returns existing

        repository.updateQuantity("item-1", 4.0, "cloves")

        coVerify(exactly = 1) { pantryDao.upsert(match { it.quantity == 4.0 && it.unit == "cloves" }) }
    }

    @Test
    fun `updateQuantity does nothing when the item no longer exists`() = runTest {
        coEvery { pantryDao.getById("missing") } returns null

        repository.updateQuantity("missing", 1.0, null)

        coVerify(exactly = 0) { pantryDao.upsert(any()) }
    }

    @Test
    fun `autocomplete returns Spoonacular's suggestions`() = runTest {
        coEvery { spoonacularApi.autocompleteIngredient("tom") } returns listOf(
            SpoonacularAutocompleteItem("tomato"), SpoonacularAutocompleteItem("tomatillo")
        )

        val result = repository.autocomplete("tom")

        assertEquals(listOf("tomato", "tomatillo"), result)
    }

    @Test
    fun `autocomplete returns an empty list instead of throwing when Spoonacular is unreachable`() = runTest {
        coEvery { spoonacularApi.autocompleteIngredient(any()) } throws java.io.IOException("no network")

        val result = repository.autocomplete("tom")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseVoiceTranscript uses Spoonacular's parser when reachable`() = runTest {
        coEvery { spoonacularApi.parseIngredients(any(), any()) } returns listOf(
            SpoonacularParsedIngredient(name = "onion", amount = 2.0, unit = "each")
        )

        val result = repository.parseVoiceTranscript("two onions")

        assertTrue(result is PantryResult.Success)
        val items = (result as PantryResult.Success).data
        assertEquals("onion", items.first().name)
        assertEquals(2.0, items.first().quantity, 0.0)
    }

    @Test
    fun `parseVoiceTranscript falls back to a naive split when Spoonacular is unreachable`() = runTest {
        coEvery { spoonacularApi.parseIngredients(any(), any()) } throws java.io.IOException("no network")

        val result = repository.parseVoiceTranscript("onion, garlic and basil")

        assertTrue(result is PantryResult.Success)
        val names = (result as PantryResult.Success).data.map { it.name }
        assertEquals(listOf("onion", "garlic", "basil"), names)
    }
}
