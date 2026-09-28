package com.ng.s33986010.medtrack.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.model.SymptomEntity
import com.ng.s33986010.medtrack.viewmodel.SymptomsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomsScreen(patientId: String, factory: ViewModelProvider.Factory, onViewTrends: () -> Unit = {}) {
    val vm: SymptomsViewModel = viewModel(factory = factory)
    val symptoms by vm.symptoms.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(patientId) { vm.init(patientId) }
    LaunchedEffect(saveState) {
        saveState?.let { snackbar.showSnackbar(it); vm.clearSaveState() }
    }

    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val categories = listOf("Pain", "Nausea", "Dizziness", "Fatigue", "Headache", "Skin Reaction", "Other")

    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf(5f) }
    var notes by rememberSaveable { mutableStateOf("") }
    var dateTime by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    val calendar = remember { Calendar.getInstance() }

    fun showDateTimePicker() {
        DatePickerDialog(context, { _, y, m, d ->
            calendar.set(y, m, d)
            TimePickerDialog(context, { _, h, min ->
                calendar.set(Calendar.HOUR_OF_DAY, h); calendar.set(Calendar.MINUTE, min)
                dateTime = formatter.format(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun validate(): Boolean {
        val e = mutableMapOf<String, String>()
        if (selectedCategory.isBlank()) e["category"] = "Please select a category"
        if (dateTime.isBlank()) e["dateTime"] = "Please select date and time"
        errors = e
        return e.isEmpty()
    }

    val (severityLabel, severityColor, _) = severityUI(severity.toInt())

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Log Symptom", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

            item {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true,
                        label = { Text("Symptom Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        isError = errors.containsKey("category"),
                        supportingText = { errors["category"]?.let { Text(it) } },
                        modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                selectedCategory = it; expanded = false
                            })
                        }
                    }
                }
            }

            item {
                Column {
                    Surface(color = severityColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (severity <= 3f) Icons.Default.CheckCircle else Icons.Default.Warning,
                                null, tint = severityColor)
                            Text("Severity: ${severity.toInt()} ($severityLabel)", color = severityColor,
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Slider(value = severity, onValueChange = { severity = it },
                        valueRange = 1f..10f, steps = 8, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                OutlinedTextField(value = notes, onValueChange = { if (it.length <= 200) notes = it },
                    label = { Text("Additional Notes (optional)") }, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${notes.length}/200") })
            }

            item {
                OutlinedTextField(value = dateTime, onValueChange = {}, readOnly = true,
                    label = { Text("When did this occur? *") },
                    isError = errors.containsKey("dateTime"),
                    supportingText = { errors["dateTime"]?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth())
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showDateTimePicker() }, modifier = Modifier.weight(1f)) {
                        Text("Pick Date & Time")
                    }
                    OutlinedButton(onClick = { dateTime = formatter.format(Date()) }, modifier = Modifier.weight(1f)) {
                        Text("Use Now")
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        if (validate()) {
                            vm.addSymptom(selectedCategory, severity.toInt(), notes, dateTime)
                            selectedCategory = ""; severity = 5f; notes = ""; dateTime = ""
                            errors = emptyMap()
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Save Symptom") }
                    OutlinedButton(onClick = {
                        selectedCategory = ""; severity = 5f; notes = ""; dateTime = ""
                        errors = emptyMap()
                    }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
            }

            item { HorizontalDivider() }

            item {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Symptom History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onViewTrends) {
                        Icon(Icons.Default.ShowChart, null)
                        Spacer(Modifier.width(4.dp))
                        Text("View Trends")
                    }
                }
            }

            if (symptoms.isEmpty()) {
                item { Text("No symptoms logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(symptoms) { SymptomCard(it) }
            }
        }
    }
}

@Composable
fun SymptomCard(symptom: SymptomEntity) {
    val (label, color, _) = severityUI(symptom.severity)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(symptom.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                Text("Severity: ${symptom.severity} ($label)", color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            Text("Date: ${symptom.dateTime}", style = MaterialTheme.typography.bodySmall)
            if (symptom.notes.isNotBlank()) Text("Notes: ${symptom.notes}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun severityUI(severity: Int): Triple<String, Color, Nothing?> = when (severity) {
    in 1..3 -> Triple("Mild", Color(0xFF4CAF50), null)
    in 4..6 -> Triple("Moderate", Color(0xFFFF9800), null)
    else -> Triple("Severe", Color(0xFFF44336), null)
}
