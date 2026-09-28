package com.ng.s33986010.medtrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ng.s33986010.medtrack.data.repository.MedTrackRepository
import com.ng.s33986010.medtrack.model.SymptomEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ViewModel

data class TrendPoint(val date: String, val avgSeverity: Float, val count: Int)
data class CategoryTrend(val category: String, val points: List<TrendPoint>, val color: Color)

class SymptomTrendViewModel(private val repo: MedTrackRepository) : ViewModel() {

    private val _trends = MutableStateFlow<List<CategoryTrend>>(emptyList())
    val trends: StateFlow<List<CategoryTrend>> = _trends

    private val _overallTrend = MutableStateFlow<List<TrendPoint>>(emptyList())
    val overallTrend: StateFlow<List<TrendPoint>> = _overallTrend

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary

    private val categoryColors = listOf(
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5),
        Color(0xFF00897B), Color(0xFFFB8C00), Color(0xFF6D4C41)
    )

    fun load(patientId: String) {
        viewModelScope.launch {
            val symptoms = repo.getSymptomsForTrend(patientId)
            if (symptoms.isEmpty()) return@launch

            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateFmt = SimpleDateFormat("MM/dd", Locale.getDefault())

            // Group by date (day only)
            val byDate = symptoms.groupBy { s ->
                try {
                    val d = fmt.parse(s.dateTime) ?: Date()
                    dateFmt.format(d)
                } catch (e: Exception) { s.dateTime.take(10) }
            }.entries.sortedBy { it.key }

            // Overall trend (all categories combined, by day)
            _overallTrend.value = byDate.map { (date, entries) ->
                TrendPoint(date, entries.map { it.severity }.average().toFloat(), entries.size)
            }

            // Per-category trend
            val categories = symptoms.map { it.category }.distinct().take(6)
            _trends.value = categories.mapIndexed { idx, cat ->
                val catSymptoms = symptoms.filter { it.category == cat }
                val catByDate = catSymptoms.groupBy { s ->
                    try {
                        val d = fmt.parse(s.dateTime) ?: Date()
                        dateFmt.format(d)
                    } catch (e: Exception) { s.dateTime.take(10) }
                }.entries.sortedBy { it.key }

                CategoryTrend(
                    category = cat,
                    points = catByDate.map { (date, entries) ->
                        TrendPoint(date, entries.map { it.severity }.average().toFloat(), entries.size)
                    },
                    color = categoryColors[idx % categoryColors.size]
                )
            }

            // Summary insight
            val overall = _overallTrend.value
            if (overall.size >= 2) {
                val first = overall.first().avgSeverity
                val last = overall.last().avgSeverity
                val diff = last - first
                _summary.value = when {
                    diff < -1f -> "📉 Your symptom severity has improved by ${"%.1f".format(-diff)} points over this period."
                    diff > 1f -> "📈 Your symptom severity has increased by ${"%.1f".format(diff)} points. Consider consulting your doctor."
                    else -> "📊 Your symptom severity has remained relatively stable."
                }
            }
        }
    }
}

// Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomTrendScreen(
    patientId: String,
    factory: ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: SymptomTrendViewModel = viewModel(factory = factory)
    val trends by vm.trends.collectAsState()
    val overallTrend by vm.overallTrend.collectAsState()
    val summary by vm.summary.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overall", "By Category")

    LaunchedEffect(patientId) { vm.load(patientId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Symptom Trends") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary card
            if (summary.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, tint) = when {
                                summary.contains("📉") -> Icons.Default.TrendingDown to Color(0xFF4CAF50)
                                summary.contains("📈") -> Icons.Default.TrendingUp to Color(0xFFF44336)
                                else -> Icons.Default.TrendingFlat to Color(0xFF2196F3)
                            }
                            Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
                            Text(
                                summary.removePrefix("📉 ").removePrefix("📈 ").removePrefix("📊 "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            if (overallTrend.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No symptom data yet. Log symptoms to see trends.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // Overall chart
                        item {
                            Text("Average Severity Over Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())) {
                                        LineChart(
                                            points = overallTrend,
                                            lineColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .width(maxOf(300.dp, (overallTrend.size * 60).dp))
                                                .height(200.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // Stats row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatChip("Min", "%.1f".format(overallTrend.minOf { it.avgSeverity }))
                                        StatChip("Max", "%.1f".format(overallTrend.maxOf { it.avgSeverity }))
                                        StatChip("Avg", "%.1f".format(overallTrend.map { it.avgSeverity }.average()))
                                        StatChip("Days", overallTrend.size.toString())
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Per-category charts
                        item {
                            Text("Severity by Symptom Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                        // Legend
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Legend", style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold)
                                    trends.forEach { trend ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier
                                                .size(12.dp)
                                                .background(trend.color, shape = MaterialTheme.shapes.small))
                                            Text(trend.category, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(2.dp)) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(16.dp)) {
                                    val maxWidth = maxOf(300, trends.maxOfOrNull { it.points.size * 60 } ?: 300)
                                    MultiLineChart(
                                        trends = trends,
                                        modifier = Modifier
                                            .width(maxWidth.dp)
                                            .height(220.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Chart Composables

@Composable
fun LineChart(
    points: List<TrendPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val padLeft = 48f
        val padBottom = 40f
        val padTop = 16f
        val padRight = 16f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padBottom - padTop

        val minSev = 0f
        val maxSev = 10f

        // Grid lines at 0, 2, 4, 6, 8, 10
        for (i in 0..5) {
            val sev = i * 2f
            val y = padTop + chartH - ((sev - minSev) / (maxSev - minSev)) * chartH
            drawLine(gridColor, Offset(padLeft, y), Offset(size.width - padRight, y),
                strokeWidth = 1f)
            // Y axis labels
            drawContext.canvas.nativeCanvas.drawText(
                "${(sev).toInt()}",
                padLeft - 8f, y + 4f,
                android.graphics.Paint().apply {
                    color = labelColor; textSize = 28f; textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        if (points.size < 2) return@Canvas

        val stepX = chartW / (points.size - 1)

        // Fill under line
        val fillPath = Path()
        points.forEachIndexed { i, p ->
            val x = padLeft + i * stepX
            val y = padTop + chartH - ((p.avgSeverity - minSev) / (maxSev - minSev)) * chartH
            if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo(padLeft + (points.size - 1) * stepX, padTop + chartH)
        fillPath.lineTo(padLeft, padTop + chartH)
        fillPath.close()
        drawPath(fillPath, lineColor.copy(alpha = 0.15f))

        // Line
        val linePath = Path()
        points.forEachIndexed { i, p ->
            val x = padLeft + i * stepX
            val y = padTop + chartH - ((p.avgSeverity - minSev) / (maxSev - minSev)) * chartH
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, lineColor, style = Stroke(width = 4f))

        // Dots + X labels
        points.forEachIndexed { i, p ->
            val x = padLeft + i * stepX
            val y = padTop + chartH - ((p.avgSeverity - minSev) / (maxSev - minSev)) * chartH
            drawCircle(lineColor, radius = 6f, center = Offset(x, y))
            drawCircle(Color.White, radius = 3f, center = Offset(x, y))
            // X axis date labels (every other to avoid overlap)
            if (i % 2 == 0 || points.size <= 5) {
                drawContext.canvas.nativeCanvas.drawText(
                    p.date,
                    x, size.height - 4f,
                    android.graphics.Paint().apply {
                        color = labelColor; textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun MultiLineChart(
    trends: List<CategoryTrend>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    // Collect all unique dates sorted
    val allDates = trends.flatMap { it.points.map { p -> p.date } }.distinct().sorted()
    if (allDates.size < 2) return

    Canvas(modifier = modifier) {
        val padLeft = 48f
        val padBottom = 40f
        val padTop = 16f
        val padRight = 16f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padBottom - padTop
        val minSev = 0f; val maxSev = 10f
        val stepX = chartW / (allDates.size - 1)

        // Grid
        for (i in 0..5) {
            val sev = i * 2f
            val y = padTop + chartH - ((sev - minSev) / (maxSev - minSev)) * chartH
            drawLine(gridColor, Offset(padLeft, y), Offset(size.width - padRight, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "${sev.toInt()}", padLeft - 8f, y + 4f,
                android.graphics.Paint().apply {
                    color = labelColor; textSize = 26f; textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // X labels
        allDates.forEachIndexed { i, date ->
            if (i % 2 == 0 || allDates.size <= 5) {
                val x = padLeft + i * stepX
                drawContext.canvas.nativeCanvas.drawText(
                    date, x, size.height - 4f,
                    android.graphics.Paint().apply {
                        color = labelColor; textSize = 24f; textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        // Each category line
        trends.forEach { trend ->
            val dateToPoint = trend.points.associateBy { it.date }
            val path = Path()
            var started = false

            allDates.forEachIndexed { i, date ->
                val point = dateToPoint[date] ?: return@forEachIndexed
                val x = padLeft + i * stepX
                val y = padTop + chartH - ((point.avgSeverity - minSev) / (maxSev - minSev)) * chartH
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }

            drawPath(path, trend.color, style = Stroke(width = 3f))

            // Dots
            allDates.forEachIndexed { i, date ->
                val point = dateToPoint[date] ?: return@forEachIndexed
                val x = padLeft + i * stepX
                val y = padTop + chartH - ((point.avgSeverity - minSev) / (maxSev - minSev)) * chartH
                drawCircle(trend.color, radius = 5f, center = Offset(x, y))
                drawCircle(Color.White, radius = 2.5f, center = Offset(x, y))
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
