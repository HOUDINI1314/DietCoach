package com.dietcoach.app.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dietcoach.app.domain.StrengthCatalog
import com.dietcoach.app.domain.StrengthCategory
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrengthScreen(
    state: AppUiState,
    onAdd: (StrengthCategory, String, Int, Int, Double, Int, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var category by remember { mutableStateOf(StrengthCategory.PUSH) }
    var exercise by remember { mutableStateOf(StrengthCatalog.exercises[category]!!.first()) }
    var sets by remember { mutableStateOf("4") }
    var reps by remember { mutableStateOf("8") }
    var load by remember { mutableStateOf("60") }
    var minutes by remember { mutableStateOf("45") }
    var useAi by remember { mutableStateOf(false) }
    val exercises = StrengthCatalog.exercises[category].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("力量训练", style = MaterialTheme.typography.headlineMedium)
        Text(
            "选择类型与动作，记录组数/次数/负荷；可勾选 AI 估算消耗。${state.effectiveWeightHint()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("日期：${state.selectedDate}", style = MaterialTheme.typography.bodyMedium)

        SectionTitle("训练类型")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StrengthCategory.entries.forEach {
                FilterChip(
                    selected = category == it,
                    onClick = {
                        category = it
                        exercise = StrengthCatalog.exercises[it]?.firstOrNull() ?: "自定义动作"
                    },
                    label = { Text(it.labelZh) }
                )
            }
        }

        SectionTitle("动作")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            exercises.forEach {
                FilterChip(
                    selected = exercise == it,
                    onClick = { exercise = it },
                    label = { Text(it) }
                )
            }
        }
        if (exercise == "自定义动作" || category == StrengthCategory.OTHER) {
            OutlinedTextField(
                value = exercise,
                onValueChange = { exercise = it },
                label = { Text("动作名称") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                sets, { sets = it }, label = { Text("组数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                reps, { reps = it }, label = { Text("次数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                load, { load = it }, label = { Text("负荷 kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                minutes, { minutes = it }, label = { Text("时长 min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        val s = sets.toIntOrNull() ?: 0
        val r = reps.toIntOrNull() ?: 0
        val l = load.toDoubleOrNull() ?: 0.0
        val volume = s * r * l
        Text("容量预估：${"%.0f".format(volume)} kg（组×次×负荷）")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI 估算消耗并入库", modifier = Modifier.weight(1f))
            Switch(checked = useAi, onCheckedChange = { useAi = it })
        }
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                onAdd(
                    category,
                    exercise.trim(),
                    s.coerceAtLeast(1),
                    r.coerceAtLeast(1),
                    l.coerceAtLeast(0.0),
                    minutes.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                    useAi
                )
            },
            enabled = exercise.isNotBlank() && !state.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存本条力量记录") }

        SectionTitle("当日力量列表")
        if (state.strength.isEmpty()) {
            Text("暂无力量记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.strength.forEach { item ->
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.category.labelZh} · ${item.exerciseName}", style = MaterialTheme.typography.titleLarge)
                            Text("${item.sets}×${item.reps} @ ${item.loadKg}kg · 容量 ${"%.0f".format(item.volumeKg)}kg")
                            Text("时长 ${item.minutes} 分钟 · 消耗 ${item.kcal} kcal · ${item.source.name}")
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
            Text(
                "力量总消耗：${state.strength.sumOf { it.kcal }} kcal",
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
