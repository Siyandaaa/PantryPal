package com.motivation.pantrypal.util

import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The "match percentage" badge shown on Screen 6 (Recipe Search Results) is
 * the single most important derived value in the app - it's what makes
 * "What's in My Pantry?" useful at a glance - so it gets its own focused
 * unit test independent of any network or database dependency.
 */
class RecipeSummaryDtoTest {

    private fun recipe(used: Int, missed: Int, total: Int) = RecipeSummaryDto(
        id = 1,
        title = "Test Recipe",
        imageUrl = null,
        readyInMinutes = 20,
        usedIngredientCount = used,
        missedIngredientCount = missed,
        totalIngredientCount = total
    )

    @Test
    fun `all ingredients owned gives 100 percent match`() {
        val recipe = recipe(used = 5, missed = 0, total = 5)
        assertEquals(100, recipe.matchPercent)
    }

    @Test
    fun `half the ingredients owned gives 50 percent match`() {
        val recipe = recipe(used = 3, missed = 3, total = 6)
        assertEquals(50, recipe.matchPercent)
    }

    @Test
    fun `zero ingredients owned gives 0 percent match`() {
        val recipe = recipe(used = 0, missed = 8, total = 8)
        assertEquals(0, recipe.matchPercent)
    }

    @Test
    fun `zero total ingredients does not divide by zero`() {
        val recipe = recipe(used = 0, missed = 0, total = 0)
        assertEquals(0, recipe.matchPercent)
    }

    @Test
    fun `match percent rounds down to nearest whole number`() {
        // 2 of 3 -> 66.67% should truncate to 66, not round up to 67.
        val recipe = recipe(used = 2, missed = 1, total = 3)
        assertEquals(66, recipe.matchPercent)
    }
}
