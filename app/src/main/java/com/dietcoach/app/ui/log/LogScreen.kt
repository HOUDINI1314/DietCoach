package com.dietcoach.app.ui.log

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.domain.WorkoutIntensity
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.OfflineChip
import com.dietcoach.app.ui.components.SectionTitle
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    state: AppUiState,
    onParseAi: (String) -> Unit,
    onParsePhoto: (ByteArray, String) -> Unit,
    onConfirmAi: (MealType) -> Unit,
    onDismissAi: () -> Unit,
    onAddFood: (MealType, String, String, Int, Double, Double, Double) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onAddWorkout: (String, Int, WorkoutIntensity) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onExtraBurn: (Int) -> Unit,
    onAnalyzeWorkoutAi: (String) -> Unit
) {
    val context = LocalContext.current
    var nlp by remember { mutableStateOf("午饭：米饭一碗、青椒肉丝、可乐") }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var foodName by remember { mutableStateOf("") }
    var foodAmount by remember { mutableStateOf("1份") }
    var foodKcal by remember { mutableStateOf("") }
    var foodP by remember { mutableStateOf("") }
    var foodC by remember { mutableStateOf("") }
    var foodF by remember { mutableStateOf("") }
    var workoutName by remember { mutableStateOf("力量训练") }
    var workoutMin by remember { mutableStateOf("45") }
    var intensity by remember { mutableStateOf(WorkoutIntensity.MEDIUM) }
    var extraBurn by remember(state.selectedDate, state.stats?.totals?.extraBurnKcal) {
        mutableStateOf(state.stats?.totals?.extraBurnKcal?.toString().orEmpty())
    }
    var aiWorkoutDesc by remember { mutableStateOf("慢跑 30 分钟，配速 6:30") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun readUri(uri: Uri): Pair<ByteArray, String>? {
        return runCatching {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            bytes to mime
        }.getOrNull()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { readUri(it) }?.let { (bytes, mime) -> onParsePhoto(bytes, mime) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingPhotoUri?.let { uri ->
                readUri(uri)?.let { (bytes, mime) -> onParsePhoto(bytes, mime) }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createTempImageUri(context)
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("记录 · ${state.selectedDate}", style = MaterialTheme.typography.headlineMedium)
        OfflineChip(state.online)
        Text(
            state.effectiveWeightHint(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("一句话记餐 / 拍照识餐（VLM）")
        OutlinedTextField(
            value = nlp,
            onValueChange = { nlp = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("文字描述饮食") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onParseAi(nlp) },
                enabled = !state.busy && nlp.isNotBlank()
            ) { Text(if (state.busy) "解析中…" else "文字 AI 解析") }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !state.busy
            ) { Text("相册识图") }
            OutlinedButton(
                onClick = { launchCamera() },
                enabled = !state.busy
            ) { Text("拍照") }
        }
        OutlinedButton(onClick = onDismissAi, enabled = state.aiPreview != null) {
            Text("清空预览")
        }
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        state.aiPreview?.let { preview ->
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("AI/VLM 预览（请核对）", style = MaterialTheme.typography.titleLarge)
                    preview.notes?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    preview.items.forEach { item ->
                        Text("${item.name} ${item.amount} · ${item.kcal}kcal · P${item.proteinG}/C${item.carbG}/F${item.fatG}")
                    }
                    MealChips(mealType) { mealType = it }
                    Button(onClick = { onConfirmAi(mealType) }, modifier = Modifier.fillMaxWidth()) {
                        Text("确认入库")
                    }
                }
            }
        }

        SectionTitle("手动添加饮食")
        MealChips(mealType) { mealType = it }
        OutlinedTextField(foodName, { foodName = it }, label = { Text("食物名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(foodAmount, { foodAmount = it }, label = { Text("份量") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(foodKcal, { foodKcal = it }, label = { Text("kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(foodP, { foodP = it }, label = { Text("蛋白g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(foodC, { foodC = it }, label = { Text("碳水g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(foodF, { foodF = it }, label = { Text("脂肪g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        Button(
            onClick = {
                onAddFood(
                    mealType, foodName.trim(), foodAmount.trim(),
                    foodKcal.toIntOrNull() ?: 0,
                    foodP.toDoubleOrNull() ?: 0.0,
                    foodC.toDoubleOrNull() ?: 0.0,
                    foodF.toDoubleOrNull() ?: 0.0
                )
                foodName = ""; foodKcal = ""; foodP = ""; foodC = ""; foodF = ""
            },
            enabled = foodName.isNotBlank()
        ) { Text("添加饮食") }

        SectionTitle("当日饮食")
        state.foods.forEach { food ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${food.mealType.labelZh} · ${food.name}")
                    Text("${food.kcal} kcal · ${food.source.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDeleteFood(food.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }

        SectionTitle("AI 分析运动消耗并入库")
        OutlinedTextField(
            aiWorkoutDesc, { aiWorkoutDesc = it },
            label = { Text("描述运动，如：骑行 40 分钟中等强度") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(
            onClick = { onAnalyzeWorkoutAi(aiWorkoutDesc) },
            enabled = !state.busy && aiWorkoutDesc.isNotBlank()
        ) { Text("AI 估算并保存") }

        SectionTitle("手动添加有氧")
        OutlinedTextField(workoutName, { workoutName = it }, label = { Text("项目") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            workoutMin, { workoutMin = it }, label = { Text("分钟") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkoutIntensity.entries.forEach {
                FilterChip(
                    selected = intensity == it,
                    onClick = { intensity = it },
                    label = { Text(it.labelZh) }
                )
            }
        }
        Button(
            onClick = {
                onAddWorkout(workoutName.trim(), workoutMin.toIntOrNull() ?: 0, intensity)
            },
            enabled = workoutName.isNotBlank()
        ) { Text("添加有氧") }
        OutlinedTextField(
            extraBurn, { extraBurn = it },
            label = { Text("额外消耗 kcal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = { onExtraBurn(extraBurn.toIntOrNull() ?: 0) }) {
            Text("保存额外消耗")
        }

        SectionTitle("当日有氧")
        state.workouts.forEach { w ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(w.name)
                    Text("${w.minutes}分钟 · ${w.kcal}kcal · ${w.source.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDeleteWorkout(w.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealChips(mealType: MealType, onSelect: (MealType) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MealType.entries.forEach {
            FilterChip(
                selected = mealType == it,
                onClick = { onSelect(it) },
                label = { Text(it.labelZh) }
            )
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
