package com.motivation.pantrypal.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen 11: Profile / Settings. This is PantryPal's dedicated settings menu
 * — the app's "Application State" requirement — where dark/light theme,
 * dietary preferences and allergy restrictions are changed and saved both
 * locally (DataStore, for instant offline reads) and to the cloud REST API
 * (so "Sync Now" has something real to demonstrate on camera).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onLoggedOut: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile & Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(uiState.userName ?: "PantryPal user", style = MaterialTheme.typography.headlineMedium)
            uiState.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark mode")
                Switch(checked = uiState.darkThemeEnabled, onCheckedChange = { viewModel.setDarkTheme(it) })
            }

            Spacer(Modifier.height(24.dp))
            Text("Dietary preferences", style = MaterialTheme.typography.titleMedium)
            Text(
                "Used to filter recipe search results (Smart Filtering).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AVAILABLE_DIETS.forEach { diet ->
                    FilterChip(
                        selected = uiState.dietaryPreferences.contains(diet),
                        onClick = { viewModel.toggleDietaryPreference(diet) },
                        label = { Text(diet) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Allergies & restrictions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AVAILABLE_ALLERGENS.forEach { allergen ->
                    FilterChip(
                        selected = uiState.allergies.contains(allergen),
                        onClick = { viewModel.toggleAllergy(allergen) },
                        label = { Text(allergen) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            uiState.statusMessage?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { viewModel.refreshFromServer() },
                enabled = !uiState.isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("SYNC NOW")
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showLogoutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("LOG OUT") }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You can log back in any time with your email or Google account.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout(onLoggedOut)
                }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } }
        )
    }
}
