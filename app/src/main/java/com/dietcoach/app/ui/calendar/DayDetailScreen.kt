package com.dietcoach.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.DeficitRing
import com.dietcoach.app.ui.components.MacroBar
import com.dietcoach.app.ui.components.SectionTitle
import com.dietcoach.app.ui.components.WeightTrendChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    state: AppUiState,
    onBack: () -> Unit,
    onSaveWeight: (Double) -> Unit
) {
    val ready = state.selectedDate == date
    val stats = state.stats.takeIf { ready }
    val foods = if (ready) state.foods else emptyList()
    val workouts = if (ready) state.workouts else emptyList()
    val strength = if (ready) state.strength else emptyList()
    val weight = state.weightOn(date)
    var weightInput by remember(date, weight) {
        mutableStateOf(weight?.toString().orEmpty())
    }
    val chartWeights = remember(state.weights, date) {
        state.weights.filter { it.date <= date }.takeLast(14)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(date) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeightTrendChart(
                weights = chartWeights,
                title = "体重坐标图（截至当日）",
                showLabels = true,
                tall = true
            )

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionTitle("体重")
                    Text(
                        if (weight != null) "当日体重：$weight kg" else "当日尚未记录体重",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("记录/修改 kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = { weightInput.toDoubleOrNull()?.let(onSaveWeight) },
                            enabled = weightInput.toDoubleOrNull() != null
                        ) { Text("保存") }
                    }
                    Text(
                        "修改体重后，AI/VLM 提示词会同步使用最新体重。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "缺口=BMR${stats.bmr}+训练${stats.totals.totalBurn}−摄入${stats.totals.intakeKcal}（有氧${stats.totals.burnKcal}/力量${stats.totals.strengthKcal}/额外${stats.totals.extraBurnKcal}）",
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

            SectionTitle("饮食详情")
            if (!ready) {
                Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (foods.isEmpty()) {
                Text("当天没有饮食记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                MealType.entries.forEach { meal ->
                    val items = foods.filter { it.mealType == meal }
                    if (items.isNotEmpty()) {
                        Text(meal.labelZh, style = MaterialTheme.typography.titleLarge)
                        items.forEach { food ->
                            Surface(
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${food.amount} · ${food.kcal} kcal · ${food.source.name}")
                                    Text(
                                        "蛋白 ${"%.1f".format(food.proteinG)}g · 碳水 ${"%.1f".format(food.carbG)}g · 脂肪 ${"%.1f".format(food.fatG)}g",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionTitle("有氧/运动详情")
            if (!ready) {
                Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (workouts.isEmpty()) {
                Text("当天没有有氧记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                workouts.forEach { w ->
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(w.name, style = MaterialTheme.typography.titleLarge)
                            Text("时长：${w.minutes} 分钟 · 强度：${w.intensity.labelZh}")
                            Text("消耗：${w.kcal} kcal", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            SectionTitle("力量训练详情")
            if (!ready) {
                Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (strength.isEmpty()) {
                Text("当天没有力量记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                strength.forEach { s ->
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${s.category.labelZh} · ${s.exerciseName}", style = MaterialTheme.typography.titleLarge)
                            Text("${s.sets}组 × ${s.reps}次 @ ${s.loadKg}kg")
                            Text("容量 ${"%.0f".format(s.volumeKg)}kg · ${s.minutes}分钟 · 消耗 ${s.kcal}kcal")
                        }
                    }
                }
            }
        }
    }
}
