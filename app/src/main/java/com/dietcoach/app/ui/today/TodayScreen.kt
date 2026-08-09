package com.dietcoach.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.DeficitRing
import com.dietcoach.app.ui.components.MacroBar
import com.dietcoach.app.ui.components.OfflineChip
import com.dietcoach.app.ui.components.SectionTitle
import com.dietcoach.app.ui.components.WeightTrendChart

@Composable
fun TodayScreen(
    state: AppUiState,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val stats = state.stats
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevDay) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前一天")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DietCoach", style = MaterialTheme.typography.headlineMedium)
                Text(state.selectedDate, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onToday) { Text("回到今天") }
            }
            IconButton(onClick = onNextDay) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "后一天")
            }
        }
        OfflineChip(state.online)

        if (stats != null) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DeficitRing(
                        deficit = stats.deficit,
                        targetIntake = stats.target.calories,
                        intake = stats.totals.intakeKcal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "缺口 = BMR ${stats.bmr} + 训练 ${stats.totals.totalBurn} − 摄入 ${stats.totals.intakeKcal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "训练：有氧 ${stats.totals.burnKcal} · 力量 ${stats.totals.strengthKcal} · 额外 ${stats.totals.extraBurnKcal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionTitle("三大营养素")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroBar("蛋白质", stats.totals.proteinG, stats.target.proteinG, Color(0xFF2D6A4F))
                MacroBar("碳水", stats.totals.carbG, stats.target.carbG, Color(0xFF40916C))
                MacroBar("脂肪", stats.totals.fatG, stats.target.fatG, Color(0xFFBC4749))
            }
        }

        SectionTitle("今日饮食")
        if (state.foods.isEmpty()) {
            Text(
                "还没有记录。去「记录」页手动添加，或用一句话让 Qwen 解析。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.foods.forEach { food ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${food.mealType.labelZh} · ${food.name}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${food.amount} · ${food.kcal} kcal · P${food.proteinG.toInt()}/C${food.carbG.toInt()}/F${food.fatG.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        SectionTitle("今日有氧")
        if (state.workouts.isEmpty()) {
            Text("暂无有氧记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.workouts.forEach { w ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(w.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${w.minutes} 分钟 · ${w.intensity.labelZh}强度 · ${w.kcal} kcal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        SectionTitle("今日力量")
        if (state.strength.isEmpty()) {
            Text("暂无力量记录，去「力量」页添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.strength.forEach { s ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${s.category.labelZh} · ${s.exerciseName}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${s.sets}组×${s.reps}次 · ${s.loadKg}kg · 容量 ${"%.0f".format(s.volumeKg)} · ${s.minutes}分钟 · ${s.kcal} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                "力量合计 ${state.strength.sumOf { it.kcal }} kcal（已计入上方缺口与消耗）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        WeightTrendChart(
            weights = state.weights.takeLast(14),
            title = "体重趋势",
            showLabels = true
        )

        val byMeal = MealType.entries.associateWith { type ->
            state.foods.filter { it.mealType == type }.sumOf { it.kcal }
        }
        Text(
            "餐次热量：早${byMeal[MealType.BREAKFAST] ?: 0} / 午${byMeal[MealType.LUNCH] ?: 0} / 晚${byMeal[MealType.DINNER] ?: 0} / 加${byMeal[MealType.SNACK] ?: 0}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
