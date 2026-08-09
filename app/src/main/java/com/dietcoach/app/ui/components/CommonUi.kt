package com.dietcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietcoach.app.data.model.WeightLogEntity

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MacroBar(label: String, current: Double, target: Int, color: Color) {
    val safeCurrent = if (current.isFinite()) current else 0.0
    val raw = if (target <= 0) 0f else (safeCurrent / target).toFloat()
    val progress = if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                "${safeCurrent.toInt()} / ${target}g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun DeficitRing(deficit: Int, targetIntake: Int, intake: Int) {
    val raw = if (targetIntake <= 0) 0f else intake.toFloat() / targetIntake
    val progress = if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 14.dp.toPx()
            val dim = size.minDimension
            if (dim <= 0f) return@Canvas
            drawArc(
                color = Color(0x332D6A4F),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(dim, dim)
            )
            drawArc(
                color = Color(0xFF2D6A4F),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(dim, dim)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("热量缺口", style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (deficit >= 0) "+$deficit kcal" else "$deficit kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (deficit >= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
            )
            Text(
                "摄入 $intake / 目标 $targetIntake",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OfflineChip(online: Boolean) {
    if (!online) {
        AssistChip(
            onClick = {},
            label = { Text("离线模式 · 本地统计可用") }
        )
    }
}

@Composable
fun WeightSparkline(weights: List<WeightLogEntity>) {
    val ordered = weights
        .filter { it.weightKg.isFinite() && it.weightKg > 0 }
        .sortedBy { it.date }
    if (ordered.size < 2) {
        Text(
            "体重曲线将在记录 2 天以上后显示",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val minW = ordered.minOf { it.weightKg }
    val maxW = ordered.maxOf { it.weightKg }
    val span = (maxW - minW).coerceAtLeast(0.1)
    val lineColor = MaterialTheme.colorScheme.primary
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(12.dp)
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val stepX = size.width / (ordered.size - 1).coerceAtLeast(1)
            ordered.forEachIndexed { index, point ->
                val x = stepX * index
                val yRatio = ((point.weightKg - minW) / span).toFloat().coerceIn(0f, 1f)
                val y = size.height - yRatio * size.height
                if (!x.isFinite() || !y.isFinite()) return@forEachIndexed
                if (index > 0) {
                    val prev = ordered[index - 1]
                    val prevX = stepX * (index - 1)
                    val prevYRatio = ((prev.weightKg - minW) / span).toFloat().coerceIn(0f, 1f)
                    val prevY = size.height - prevYRatio * size.height
                    if (prevX.isFinite() && prevY.isFinite()) {
                        drawLine(
                            color = lineColor,
                            start = Offset(prevX, prevY),
                            end = Offset(x, y),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
            }
        }
    }
}
