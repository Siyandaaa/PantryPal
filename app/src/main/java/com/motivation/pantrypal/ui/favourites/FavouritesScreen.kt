package com.motivation.pantrypal.ui.favourites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 10: saved recipes grouped by user-defined collections. */
@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    onRecipeSelected: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favourites", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (uiState.recipes.isEmpty()) {
            Text("No saved recipes yet - tap the heart on any recipe to save it here.")
        } else {
            val grouped = uiState.recipes.groupBy { it.collectionName }
            LazyColumn {
                grouped.forEach { (collection, recipes) ->
                    item {
                        Text("$collection (${recipes.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(recipes) { recipe ->
                        ListItem(
                            headlineContent = { Text(recipe.title) },
                            supportingContent = { Text("${recipe.readyInMinutes} min") },
                            modifier = Modifier.clickableRow { onRecipeSelected(recipe.recipeId) }
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
