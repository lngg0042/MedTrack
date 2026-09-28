package com.ng.s33986010.medtrack.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.model.MedicationEntity
import com.ng.s33986010.medtrack.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    patientId: String,
    factory: ViewModelProvider.Factory,
    onAddMedication: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val patient by vm.patient.collectAsState()
    val medications by vm.medications.collectAsState()
    val takenMap by vm.takenMap.collectAsState()
    val saveState by vm.saveState.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(patientId) { vm.init(patientId) }

    // refresh taken states when medication list changes
    LaunchedEffect(medications) {
        if (medications.isNotEmpty()) {
            vm.refreshTakenStatus(medications.map { it.id })
        }
    }

    LaunchedEffect(saveState) {
        saveState?.let {
            snackbarHost.showSnackbar(it)
            vm.clearSaveState()
        }
    }

    val todayDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val takenCount = medications.count { takenMap[it.id] == true }
    val progress = if (medications.isNotEmpty()) takenCount.toFloat() / medications.size else 0f

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hello, ${patient?.name ?: "Patient"}",
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(todayDate, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Patient ID: $patientId", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("$takenCount of ${medications.size} medications taken")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    }
                }
            }

            item {
                Button(onClick = onAddMedication, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Medication")
                }
            }

            item {
                Text("Today's Medications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (medications.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Medication, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("No medications scheduled.")
                        }
                    }
                }
            } else {
                items(medications) { med ->
                    MedicationCard(
                        medication = med,
                        isTaken = takenMap[med.id] == true,
                        onTakenChange = { vm.setTaken(med.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun MedicationCard(
    medication: MedicationEntity,
    isTaken: Boolean,
    onTakenChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isTaken) 0.75f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(medication.medicationName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None)
                    if (isTaken) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                val deco = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                Text("Dosage: ${medication.dosage}", textDecoration = deco)
                Text("Frequency: ${medication.frequency}", textDecoration = deco)
                Text("Time: ${medication.scheduledTime}", textDecoration = deco)
                if (medication.medicationType.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    AssistChip(onClick = {}, label = { Text(medication.medicationType) })
                }
                if (medication.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Notes: ${medication.notes}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = deco)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(checked = isTaken, onCheckedChange = onTakenChange)
                Text("Taken", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ─── Add Medication Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    patientId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val saveState by vm.saveState.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(patientId) { vm.init(patientId) }

    LaunchedEffect(saveState) {
        saveState?.let {
            snackbarHost.showSnackbar(it)
            vm.clearSaveState()
            onBack()
        }
    }

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var freqExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    val frequencyOptions = listOf("Once daily", "Twice daily", "Three times daily", "As needed")
    val typeOptions = listOf("Tablet", "Capsule", "Liquid", "Injection", "Topical", "Other")
    val dosageRegex = Regex("^\\d+(\\.\\d+)?(mg|ml|g)\$", RegexOption.IGNORE_CASE)

    val calendar = Calendar.getInstance()
    val timePicker = TimePickerDialog(
        context,
        { _, hour, min -> time = String.format(Locale.getDefault(), "%02d:%02d", hour, min) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    fun validate(): Boolean {
        val e = mutableMapOf<String, String>()
        if (name.isBlank()) e["name"] = "Required"
        if (dosage.isBlank()) e["dosage"] = "Required"
        else if (!dosageRegex.matches(dosage.trim())) e["dosage"] = "e.g. 500mg, 10ml, 1.5g"
        if (frequency.isBlank()) e["frequency"] = "Required"
        if (time.isBlank()) e["time"] = "Required"
        if (type.isBlank()) e["type"] = "Required"
        errors = e
        return e.isEmpty()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(title = { Text("Add Medication") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Medication Name *") }, modifier = Modifier.fillMaxWidth(),
                isError = errors.containsKey("name"),
                supportingText = { errors["name"]?.let { Text(it) } })

            OutlinedTextField(value = dosage, onValueChange = { dosage = it },
                label = { Text("Dosage *") }, placeholder = { Text("e.g. 500mg") },
                modifier = Modifier.fillMaxWidth(), isError = errors.containsKey("dosage"),
                supportingText = { errors["dosage"]?.let { Text(it) } })

            // Frequency dropdown
            ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = it }) {
                OutlinedTextField(value = frequency, onValueChange = {}, readOnly = true,
                    label = { Text("Frequency *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(freqExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    isError = errors.containsKey("frequency"))
                ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                    frequencyOptions.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { frequency = it; freqExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = time, onValueChange = { time = it },
                label = { Text("Scheduled Time *") }, placeholder = { Text("HH:mm") },
                modifier = Modifier.fillMaxWidth(), isError = errors.containsKey("time"),
                supportingText = { errors["time"]?.let { Text(it) } },
                trailingIcon = { IconButton(onClick = { timePicker.show() }) {
                    Icon(Icons.Default.AccessTime, null) } })

            // Type dropdown
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(value = type, onValueChange = {}, readOnly = true,
                    label = { Text("Medication Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    isError = errors.containsKey("type"))
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    typeOptions.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { type = it; typeExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (validate()) vm.addMedication(name, dosage, frequency, time, type, notes)
                }, modifier = Modifier.weight(1f)) { Text("Save") }
                OutlinedButton(onClick = {
                    name = ""; dosage = ""; frequency = ""; time = ""; type = ""; notes = ""
                    errors = emptyMap()
                }, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
        }
    }
}
