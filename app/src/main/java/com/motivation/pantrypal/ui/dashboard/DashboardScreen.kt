package com.motivation.pantrypal.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 4 (Main Dashboard). Home tab content; bottom nav itself lives in PantryPalNavHost. */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenPantry: () -> Unit,
    onOpenMealPlan: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onOpenFavourites: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Good morning", style = MaterialTheme.typography.bodyMedium)
        Text(
            "What's cooking, ${uiState.userName?.substringBefore(" ") ?: "there"}?",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(96.dp)) {
            items(quickActions(onOpenPantry, onOpenMealPlan, onOpenShoppingList, onOpenFavourites)) { action ->
                QuickActionButton(action)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("${uiState.pantryItems.size}", "Pantry Items", Modifier.weight(1f))
            StatCard("-", "Planned Meals", Modifier.weight(1f))
            StatCard("-", "Saved Recipes", Modifier.weight(1f))
        }
    }
}

private data class QuickAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

private fun quickActions(
    onOpenPantry: () -> Unit,
    onOpenMealPlan: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onOpenFavourites: () -> Unit
) = listOf(
    QuickAction("Pantry", Icons.Filled.Kitchen, onOpenPantry),
    QuickAction("Plan", Icons.Filled.CalendarMonth, onOpenMealPlan),
    QuickAction("List", Icons.Filled.ShoppingCart, onOpenShoppingList),
    QuickAction("Favourites", Icons.Filled.Favorite, onOpenFavourites)
)

@Composable
private fun QuickActionButton(action: QuickAction) {
    Column(
        modifier = Modifier.padding(4.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(onClick = action.onClick) {
            Icon(action.icon, contentDescription = action.label)
        }
        Text(action.label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
