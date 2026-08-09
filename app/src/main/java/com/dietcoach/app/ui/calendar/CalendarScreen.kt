package com.dietcoach.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.SectionTitle
import com.dietcoach.app.ui.components.WeightTrendChart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    state: AppUiState,
    onOpenDay: (String) -> Unit,
    onLogWeightToday: (Double) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var quickWeight by remember {
        mutableStateOf(state.weightOn(LocalDate.now().toString())?.toString().orEmpty())
    }
    val today = remember { LocalDate.now().toString() }
    val titleFmt = remember {
        DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
    }
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val cells = remember(month) { buildMonthCells(month) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("日历", style = MaterialTheme.typography.headlineMedium)
        Text(
            "格子里显示体重；点日期查看当天饮食与运动详情。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("今日称重")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickWeight,
                        onValueChange = { quickWeight = it },
                        label = { Text("体重 kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            quickWeight.toDoubleOrNull()?.let(onLogWeightToday)
                        },
                        enabled = quickWeight.toDoubleOrNull() != null
                    ) { Text("记录") }
                }
                state.weightOn(today)?.let {
                    Text(
                        "今日已记录：$it kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
            }
            Text(
                month.format(titleFmt),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        weight = cell?.let { state.weightOn(it) },
                        hasActivity = cell != null && cell in state.activeDates,
                        isToday = cell == today,
                        modifier = Modifier.weight(1f),
                        onClick = { cell?.let(onOpenDay) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "绿点表示当天有饮食/有氧/力量记录；点日期进入详情坐标图",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val monthWeights = remember(month, state.weights) {
            val start = month.atDay(1).toString()
            val end = month.atEndOfMonth().toString()
            state.weights.filter { it.date in start..end }
        }
        WeightTrendChart(
            weights = monthWeights.ifEmpty { state.weights.takeLast(30) },
            title = if (monthWeights.size >= 2) "本月体重曲线" else "体重趋势（含标注）",
            showLabels = true,
            tall = false
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CalendarDayCell(
    cell: String?,
    weight: Double?,
    hasActivity: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = when {
        cell == null -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        isToday -> MaterialTheme.colorScheme.primaryContainer
        weight != null -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(shape)
            .background(bg)
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .then(if (cell != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
    ) {
        if (cell != null) {
            val dayNum = cell.takeLast(2).trimStart('0').ifEmpty { "0" }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dayNum,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = weight?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "·",
                    fontSize = 11.sp,
                    color = if (weight != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasActivity) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                )
            }
        }
    }
}

private fun buildMonthCells(month: YearMonth): List<String?> {
    val first = month.atDay(1)
    // Monday=1 ... Sunday=7 in ISO; shift so Monday is first column
    val lead = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val cells = MutableList<String?>(lead) { null }
    for (d in 1..days) {
        cells += month.atDay(d).toString()
    }
    while (cells.size % 7 != 0) cells += null
    return cells
}
