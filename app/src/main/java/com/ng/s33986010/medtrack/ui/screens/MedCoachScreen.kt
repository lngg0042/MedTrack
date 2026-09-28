package com.ng.s33986010.medtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.network.DrugLabelResult
import com.ng.s33986010.medtrack.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedCoachScreen(
    patientId: String,
    factory: ViewModelProvider.Factory,
    geminiApiKey: String
) {
    val vm: MedCoachViewModel = viewModel(factory = factory)
    val drugState by vm.drugState.collectAsState()
    val tipState by vm.tipState.collectAsState()
    val tips by vm.tips.collectAsState()
    val patientMedNames by vm.patientMedNames.collectAsState()

    LaunchedEffect(patientId) { vm.init(patientId, geminiApiKey) }

    var drugQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }

    if (showTipsDialog) {
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = { Text("All Tips History") },
            text = {
                if (tips.isEmpty()) {
                    Text("No tips generated yet.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tips) { tip ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(tip.tipText, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(tip.timestamp, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTipsDialog = false }) { Text("Close") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DRUG INFO SECTION
        item {
            Text("Drug Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (patientMedNames.isNotEmpty()) {
                    Text("Your Medications:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                        OutlinedTextField(
                            value = drugQuery, onValueChange = { drugQuery = it },
                            label = { Text("Search drug or select yours") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            patientMedNames.forEach { medName ->
                                DropdownMenuItem(text = { Text(medName) }, onClick = {
                                    drugQuery = medName; dropdownExpanded = false
                                })
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = drugQuery, onValueChange = { drugQuery = it },
                        label = { Text("Enter drug name") }, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = { vm.searchDrug(drugQuery) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = drugQuery.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search Drug Info")
                }
            }
        }

        item {
            when (val state = drugState) {
                is DrugSearchState.Idle -> {}
                is DrugSearchState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DrugSearchState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                when {
                                    state.message.contains("found", ignoreCase = true) -> "Drug not found. Try a different spelling."
                                    state.message.contains("network", ignoreCase = true) || state.message.contains("connect", ignoreCase = true) ->
                                        "Network error. Check your connection."
                                    else -> "Could not retrieve drug info: ${state.message}"
                                },
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is DrugSearchState.Success -> DrugInfoCard(state.result)
            }
        }

        item { HorizontalDivider() }

        // GENAI TIPS SECTION
        item {
            Text("MedCoach AI Tips", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.generateTip() }, modifier = Modifier.weight(1f),
                    enabled = tipState !is TipState.Loading) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Get Personalised Tip")
                }
                OutlinedButton(onClick = { showTipsDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(4.dp))
                    Text("All Tips")
                }
            }
        }

        item {
            when (val state = tipState) {
                is TipState.Idle -> {}
                is TipState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Generating personalised tip...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is TipState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text("Error: ${state.message}", modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                is TipState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Your Personalised Tip", fontWeight = FontWeight.Bold)
                            }
                            Text(state.tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrugInfoCard(result: DrugLabelResult) {
    val brandName = result.openfda?.brandName?.firstOrNull() ?: "Unknown"
    val genericName = result.openfda?.genericName?.firstOrNull() ?: ""

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(brandName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (genericName.isNotBlank()) {
                Text("Generic: $genericName", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            DrugInfoField("Purpose / Indication",
                result.purpose?.firstOrNull() ?: result.indicationsAndUsage?.firstOrNull() ?: "Not available")

            DrugInfoField("Warnings",
                result.warnings?.firstOrNull() ?: "Not available")

            DrugInfoField("Dosage & Administration",
                result.dosageAndAdministration?.firstOrNull() ?: "Not available")

            result.activeIngredient?.firstOrNull()?.let {
                DrugInfoField("Active Ingredient", it)
            }

            result.storageAndHandling?.firstOrNull()?.let {
                DrugInfoField("Storage", it)
            }
        }
    }
}

@Composable
fun DrugInfoField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        // Truncate very long values for readability
        val displayValue = if (value.length > 300) value.take(300) + "..." else value
        Text(displayValue, style = MaterialTheme.typography.bodySmall)
    }
}
