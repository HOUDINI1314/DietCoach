package com.dietcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dietcoach.app.data.model.WeightLogEntity
import java.util.Locale
import kotlin.math.max

@Composable
fun WeightTrendChart(
    weights: List<WeightLogEntity>,
    modifier: Modifier = Modifier,
    title: String = "体重趋势",
    showLabels: Boolean = true,
    tall: Boolean = false,
    maxPoints: Int = 21
) {
    val ordered = remember(weights, maxPoints) {
        weights
            .asSequence()
            .filter { it.weightKg.isFinite() && it.weightKg > 0 }
            .sortedBy { it.date }
            .toList()
            .let { if (it.size <= maxPoints) it else it.takeLast(maxPoints) }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (ordered.size < 2) {
            Text(
                "至少记录 2 天体重后显示曲线（当前 ${ordered.size} 天）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val minW = ordered.minOf { it.weightKg }
        val maxW = ordered.maxOf { it.weightKg }
        val span = max(maxW - minW, 0.5)
        val lineColor = MaterialTheme.colorScheme.primary
        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        val chartHeight = if (tall) 220.dp else 140.dp
        // 点多时隔点标注，减轻绘制与滚动卡顿
        val labelStride = remember(ordered.size, showLabels) {
            when {
                !showLabels -> Int.MAX_VALUE
                ordered.size <= 8 -> 1
                ordered.size <= 14 -> 2
                else -> 3
            }
        }
        val labelPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1B4332")
                textSize = 28f
            }
        }
        val axisPaint = remember {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#5C6F66")
                textSize = 26f
            }
        }
        val maxLabel = remember(maxW) { String.format(Locale.CHINA, "%.1f", maxW) }
        val minLabel = remember(minW) { String.format(Locale.CHINA, "%.1f", minW) }

        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 28.dp)
            ) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val leftPad = 36f
                val plotW = size.width - leftPad
                val plotH = size.height
                val stepX = if (ordered.size == 1) 0f else plotW / (ordered.size - 1)

                for (i in 0..3) {
                    val y = plotH * i / 3f
                    drawLine(axisColor, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
                }

                val path = Path()
                val last = ordered.lastIndex
                ordered.forEachIndexed { index, point ->
                    val x = leftPad + stepX * index
                    val yRatio = ((point.weightKg - minW) / span).toFloat().coerceIn(0f, 1f)
                    val y = plotH - yRatio * plotH
                    val offset = Offset(x, y)
                    if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                    drawCircle(lineColor, radius = 5f, center = offset)
                    if (showLabels && (index % labelStride == 0 || index == last)) {
                        val label = String.format(Locale.CHINA, "%.1f", point.weightKg)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            offset.x - 18f,
                            offset.y - 12f,
                            labelPaint
                        )
                    }
                }
                drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

                drawContext.canvas.nativeCanvas.drawText(maxLabel, 0f, 22f, axisPaint)
                drawContext.canvas.nativeCanvas.drawText(minLabel, 0f, plotH, axisPaint)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "起 ${ordered.first().date.takeLast(5)} · ${ordered.first().weightKg}kg",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val delta = ordered.last().weightKg - ordered.first().weightKg
            Text(
                "止 ${ordered.last().date.takeLast(5)} · ${ordered.last().weightKg}kg（${if (delta >= 0) "+" else ""}${"%.1f".format(delta)}）",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
