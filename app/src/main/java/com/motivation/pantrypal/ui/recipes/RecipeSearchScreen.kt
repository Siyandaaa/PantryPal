package com.motivation.pantrypal.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.motivation.pantrypal.data.remote.dto.RecipeSummaryDto

/** Screen 6: recipe results ranked against the pantry, with smart filters. */
@Composable
fun RecipeSearchScreen(
    viewModel: RecipeSearchViewModel,
    onRecipeSelected: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Results (${uiState.results.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(null, "vegetarian", "gluten free", "vegan")) { diet ->
                FilterChip(
                    selected = uiState.selectedDiet == diet,
                    onClick = { viewModel.setDietFilter(diet) },
                    label = { Text(diet?.replaceFirstChar { it.uppercase() } ?: "All") }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.errorMessage != null -> Text(
                "Couldn't load recipes: ${uiState.errorMessage}",
                color = MaterialTheme.colorScheme.error
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.results, key = { it.id }) { recipe ->
                    RecipeResultCard(recipe, onClick = { onRecipeSelected(recipe.id) })
                }
            }
        }
    }
}

@Composable
private fun RecipeResultCard(recipe: RecipeSummaryDto, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.title,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                val matchLabel = if (recipe.missedIngredientCount == 0) "\u2713 PERFECT!"
                else "${recipe.usedIngredientCount}/${recipe.totalIngredientCount} used"
                Text(matchLabel, style = MaterialTheme.typography.labelSmall)
                // findByIngredients doesn't return prep time or cuisine - only the
                // recipe detail endpoint does - so this line only appears once
                // those fields are known (i.e. after a diet filter's bulk lookup).
                val readyLabel = if (recipe.readyInMinutes > 0) "${recipe.readyInMinutes} min" else null
                val metaLabel = listOfNotNull(readyLabel, recipe.cuisine).joinToString("  \u2022  ")
                if (metaLabel.isNotEmpty()) {
                    Text(metaLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
