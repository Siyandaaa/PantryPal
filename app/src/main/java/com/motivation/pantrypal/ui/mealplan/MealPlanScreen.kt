package com.motivation.pantrypal.ui.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter

/** Screen 8 from the design doc. */
@Composable
fun MealPlanScreen(viewModel: MealPlanViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMM d")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Meal Plan", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previousWeek) { Icon(Icons.Filled.ChevronLeft, null) }
                Text("${uiState.weekStart.format(formatter)} - ${uiState.weekStart.plusDays(6).format(formatter)}")
                IconButton(onClick = viewModel::nextWeek) { Icon(Icons.Filled.ChevronRight, null) }
            }
        }
        Spacer(Modifier.height(16.dp))

        listOf("BREAKFAST", "LUNCH", "DINNER").forEach { slot ->
            Text(slot, style = MaterialTheme.typography.labelSmall)
            val slotEntries = uiState.entries.filter { it.mealSlot == slot }
            if (slotEntries.isEmpty()) {
                Text("No meals planned yet", style = MaterialTheme.typography.bodyMedium)
            } else {
                slotEntries.forEach { entry ->
                    ListItem(
                        headlineContent = { Text(entry.recipeTitle) },
                        supportingContent = { Text(entry.planDate) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.removeEntry(entry.id) }) { Text("Remove") }
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = viewModel::generateShoppingList,
            enabled = !uiState.isGeneratingList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isGeneratingList) "Generating..." else "GENERATE LIST")
        }
    }
}
