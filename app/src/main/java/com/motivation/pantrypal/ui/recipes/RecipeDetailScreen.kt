package com.motivation.pantrypal.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Screen 7: full recipe detail with nutrition bars, ingredients and plan/list actions. */

private val MEAL_SLOTS = listOf("BREAKFAST", "LUNCH", "DINNER")
private fun String.toTitleCase() = lowercase().replaceFirstChar { it.titlecase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPlanDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedSlot by remember { mutableStateOf("DINNER") }

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.errorMessage != null -> Text("Couldn't load recipe: ${uiState.errorMessage}")
        uiState.recipe != null -> {
            val recipe = uiState.recipe!!
            Column(modifier = Modifier.fillMaxSize()) {
                Box {
                    AsyncImage(model = recipe.imageUrl, contentDescription = recipe.title, modifier = Modifier.fillMaxWidth().height(220.dp))
                    IconButton(onClick = { viewModel.toggleFavourite() }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Save to favourites")
                    }
                }
                Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
                    Text(recipe.cuisine ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip("${recipe.readyInMinutes} min", "Time")
                        StatChip("${recipe.servings}", "Servings")
                        StatChip("${recipe.nutrition.calories}", "Cal")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                    NutrientBar("Protein", recipe.nutrition.protein, 100.0)
                    NutrientBar("Carbs", recipe.nutrition.carbs, 200.0)
                    NutrientBar("Fat", recipe.nutrition.fat, 80.0)
                    NutrientBar("Fibre", recipe.nutrition.fibre, 30.0)

                    Spacer(Modifier.height(16.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 220.dp)) {
                        items(recipe.ingredients) { ingredient ->
                            Text(
                                "${if (ingredient.ownedByUser) "\u2713" else "\u2717"} ${ingredient.amount} ${ingredient.unit.orEmpty()} ${ingredient.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showPlanDialog = true }, modifier = Modifier.weight(1f)) { Text("ADD TO PLAN") }
                    OutlinedButton(onClick = { showPlanDialog = true }, modifier = Modifier.weight(1f)) { Text("ADD TO LIST") }
                }
                uiState.addedToPlanMessage?.let {
                    Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            if (showPlanDialog) {
                AlertDialog(
                    onDismissRequest = { showPlanDialog = false },
                    title = { Text("Add to meal plan") },
                    text = {
                        Column {
                            Text("Day", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Meal", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MEAL_SLOTS.forEach { slot ->
                                    FilterChip(
                                        selected = selectedSlot == slot,
                                        onClick = { selectedSlot = slot },
                                        label = { Text(slot.toTitleCase()) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addToPlan(selectedDate.toString(), selectedSlot)
                            showPlanDialog = false
                        }) { Text("Confirm") }
                    },
                    dismissButton = { TextButton(onClick = { showPlanDialog = false }) { Text("Cancel") } }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NutrientBar(label: String, value: Double, max: Double) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value}g", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(progress = { (value / max).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}
