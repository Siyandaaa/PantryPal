package com.motivation.pantrypal.ui.shoppinglist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 9: consolidated, aisle-organised shopping list. */
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }
    val remaining = uiState.items.count { !it.isChecked }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Shopping List", style = MaterialTheme.typography.titleLarge)
                Text("$remaining items remaining \u00b7 ${uiState.items.size - remaining} checked", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = viewModel::regenerate) { Text("\u21bb Regenerate") }
        }
        Spacer(Modifier.height(12.dp))

        val grouped = uiState.items.groupBy { it.aisle }
        LazyColumn(modifier = Modifier.weight(1f)) {
            grouped.forEach { (aisle, items) ->
                item { Text(aisle.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 6.dp)) }
                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.isChecked, onCheckedChange = { viewModel.toggleChecked(item) })
                        Column {
                            Text(
                                item.ingredientName,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                            )
                            Text("${item.quantity} ${item.unit.orEmpty()}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(newItem, { newItem = it }, label = { Text("Add item manually") }, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.addManualItem(newItem, 1.0, null); newItem = "" }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    }
}
