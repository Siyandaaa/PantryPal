package com.motivation.pantrypal.ui.pantry

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

/**
 * Screen 5: "My Pantry". Ingredients are grouped by category; users can type
 * (with autocomplete), speak via [RecognizerIntent], or remove items. Voice
 * input satisfies the "Integrate external libraries / SDKs" (Android
 * SpeechRecognizer) requirement from Part 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    viewModel: PantryViewModel,
    onFindRecipes: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newItemName by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) viewModel.onVoiceTranscript(spoken)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setListening(true)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the ingredients you have, e.g. 'two onions and a bunch of basil'")
            }
            speechLauncher.launch(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Pantry") },
                actions = {
                    IconButton(onClick = { micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice add ingredient")
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ingredient")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onFindRecipes, text = { Text("FIND RECIPES") }, icon = {})
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Text(
                    "${uiState.items.size} Ingredients in your pantry today",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(12.dp))

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            val grouped = uiState.items.groupBy { it.category }
            LazyColumn(modifier = Modifier.weight(1f)) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(
                            category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${item.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }} ${item.unit.orEmpty()}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            IconButton(onClick = { viewModel.removeIngredient(item.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove ${item.name}")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Add ingredient", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = {
                        newItemName = it
                        viewModel.onSearchQueryChanged(it)
                    },
                    label = { Text("Ingredient name") },
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.autocompleteSuggestions.forEach { suggestion ->
                    TextButton(onClick = { newItemName = suggestion }) { Text(suggestion) }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.addIngredientManually(newItemName, quantity = 1.0, unit = null, category = "Uncategorised")
                        newItemName = ""
                        showAddSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add to pantry") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
