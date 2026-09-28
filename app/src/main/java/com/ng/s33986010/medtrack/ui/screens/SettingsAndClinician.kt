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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.viewmodel.*

// Settings Screen
@Composable
fun SettingsScreen(
    patientId: String,
    factory: ViewModelProvider.Factory,
    authViewModel: AuthViewModel,
    onClinicianLogin: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val patient by vm.patient.collectAsState()

    LaunchedEffect(patientId) { vm.load(patientId) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Account Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                ProfileRow(icon = Icons.Default.Person, label = "Name", value = patient?.name ?: "-")
                ProfileRow(icon = Icons.Default.Phone, label = "Phone", value = patient?.phoneNumber ?: "-")
                ProfileRow(icon = Icons.Default.Badge, label = "Patient ID", value = patientId)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { vm.logout { authViewModel.logout() } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }

        OutlinedButton(onClick = onClinicianLogin, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MedicalServices, null)
            Spacer(Modifier.width(8.dp))
            Text("Clinician Login")
        }
    }
}

@Composable
fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

// Clinician Login Screen

private const val CLINICIAN_ACCESS_KEY = "dollar-entry-apples"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianLoginScreen(onSuccess: () -> Unit, onBack: () -> Unit) {
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Clinician Access") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MedicalServices, null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Clinician Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Enter the access key to continue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            if (error.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(error, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(value = key, onValueChange = { key = it; error = "" },
                label = { Text("Access Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                if (key == CLINICIAN_ACCESS_KEY) onSuccess()
                else error = "Invalid access key"
            }, modifier = Modifier.fillMaxWidth()) { Text("Enter Dashboard") }
        }
    }
}

// Clinician Dashboard Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianDashboardScreen(
    factory: ViewModelProvider.Factory,
    geminiApiKey: String,
    onBack: () -> Unit
) {
    val vm: ClinicianViewModel = viewModel(factory = factory)
    val stats by vm.stats.collectAsState()
    val insightState by vm.insightState.collectAsState()

    LaunchedEffect(Unit) { vm.load(geminiApiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinician Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Aggregate Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                if (stats == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val s = stats!!
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatRow(Icons.Default.People, "Total Patients", s.totalPatients.toString())
                            StatRow(Icons.Default.Medication, "Avg. Medications/Patient", "%.1f".format(s.avgMedsPerPatient))
                            StatRow(Icons.Default.HealthAndSafety, "Most Common Symptom", s.mostCommonSymptom)
                            StatRow(Icons.Default.TrendingUp, "Avg. Symptom Severity", "%.1f / 10".format(s.avgSymptomSeverity))
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("AI-Powered Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Button(onClick = { vm.findPatterns() }, modifier = Modifier.fillMaxWidth(),
                    enabled = insightState !is InsightState.Loading) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Find Patterns (AI)")
                }
            }

            item {
                when (val state = insightState) {
                    is InsightState.Idle -> {}
                    is InsightState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Analysing patient data...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is InsightState.Error -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Error: ${state.message}", modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    is InsightState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.insights.forEachIndexed { idx, insight ->
                                Card(modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Surface(color = MaterialTheme.colorScheme.secondary,
                                            shape = MaterialTheme.shapes.small) {
                                            Text("${idx + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                                        }
                                        Text(insight, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
