package com.dietcoach.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.domain.ActivityLevel
import com.dietcoach.app.domain.Sex
import com.dietcoach.app.ui.AppUiState
import com.dietcoach.app.ui.components.SectionTitle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: AppUiState,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onLogWeight: (Double) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenGuide: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(state.profile.name) }
    var sex by remember { mutableStateOf(state.profile.sex) }
    var ageText by remember { mutableStateOf(state.profile.age.toString()) }
    var heightText by remember { mutableStateOf(formatNumber(state.profile.heightCm)) }
    var weightText by remember { mutableStateOf(formatNumber(state.profile.weightKg)) }
    var targetWeightText by remember { mutableStateOf(formatNumber(state.profile.targetWeightKg)) }
    var kgPerWeekText by remember { mutableStateOf(formatNumber(state.profile.kgPerWeek)) }
    var proteinText by remember { mutableStateOf(formatNumber(state.profile.proteinPerKg)) }
    var activityLevel by remember { mutableStateOf(state.profile.activityLevel) }
    var qwenModel by remember { mutableStateOf(state.profile.qwenModel) }
    var weightInput by remember { mutableStateOf(formatNumber(state.profile.weightKg)) }
    var formError by remember { mutableStateOf<String?>(null) }

    // 仅在外部画像变化且与当前草稿一致来源时同步，避免编辑中途被 Double 回写打乱
    LaunchedEffect(state.profile) {
        val p = state.profile
        name = p.name
        sex = p.sex
        ageText = p.age.toString()
        heightText = formatNumber(p.heightCm)
        weightText = formatNumber(p.weightKg)
        targetWeightText = formatNumber(p.targetWeightKg)
        kgPerWeekText = formatNumber(p.kgPerWeek)
        proteinText = formatNumber(p.proteinPerKg)
        activityLevel = p.activityLevel
        qwenModel = p.qwenModel
        weightInput = formatNumber(p.weightKg)
        formError = null
    }

    var activityExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // 作为底部 Tab 时不用 TopAppBar，避免白色顶栏与背景色割裂
        if (onBack != null) {
            TopAppBar(
                title = { Text("我的") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (onOpenGuide != null) {
                        IconButton(onClick = onOpenGuide) {
                            Icon(Icons.Outlined.Info, contentDescription = "指标说明")
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (onBack == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("我的", style = MaterialTheme.typography.headlineMedium)
                        if (onOpenGuide != null) {
                            IconButton(onClick = onOpenGuide) {
                                Icon(Icons.Outlined.Info, contentDescription = "指标说明")
                            }
                        }
                    }
                }
                Text(
                    "本地优先 · Key 仅存本机 · 饮食/运动 AI 按当日体重对齐（无记录则用下方当前体重）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionTitle("身体画像")
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("昵称") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sex == Sex.MALE,
                        onClick = { sex = Sex.MALE },
                        label = { Text("男") }
                    )
                    FilterChip(
                        selected = sex == Sex.FEMALE,
                        onClick = { sex = Sex.FEMALE },
                        label = { Text("女") }
                    )
                }
                NumberField("年龄", ageText) { ageText = filterIntText(it) }
                NumberField("身高 cm", heightText) { heightText = filterDecimalText(it) }
                NumberField("当前体重 kg", weightText) { weightText = filterDecimalText(it) }
                NumberField("目标体重 kg", targetWeightText) { targetWeightText = filterDecimalText(it) }
                NumberField("每周减重 kg（0.1–1.0）", kgPerWeekText) { kgPerWeekText = filterDecimalText(it) }
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = activityLevel.labelZh,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("活动水平") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(activityExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false }
                    ) {
                        ActivityLevel.entries.forEach {
                            DropdownMenuItem(
                                text = { Text("${it.labelZh} (×${it.factor})") },
                                onClick = {
                                    activityLevel = it
                                    activityExpanded = false
                                }
                            )
                        }
                    }
                }
                NumberField("蛋白 g/kg", proteinText) { proteinText = filterDecimalText(it) }
                Text(
                    "热量缺口 = BMR + 训练消耗 − 饮食摄入（不乘活动系数；活动水平仅供参考/AI）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                formError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val parsed = parseProfileDraft(
                            base = state.profile,
                            name = name,
                            sex = sex,
                            ageText = ageText,
                            heightText = heightText,
                            weightText = weightText,
                            targetWeightText = targetWeightText,
                            kgPerWeekText = kgPerWeekText,
                            proteinText = proteinText,
                            activityLevel = activityLevel,
                            qwenModel = qwenModel
                        )
                        if (parsed == null) {
                            formError = "请检查数字：年龄/身高/体重需有效；每周减重在 0.1–1.0"
                        } else {
                            formError = null
                            onSaveProfile(parsed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存画像")
                }
            }

            item {
                SectionTitle("今日称重")
                OutlinedTextField(
                    weightInput,
                    { weightInput = filterDecimalText(it) },
                    label = { Text("体重 kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { weightInput.toDoubleOrNull()?.let(onLogWeight) },
                    enabled = weightInput.toDoubleOrNull() != null
                ) { Text("记录体重") }
            }

            item {
                SectionTitle("Qwen / DashScope")
                Text(
                    if (state.hasApiKey) "已配置 API Key" else "尚未配置 API Key",
                    color = if (state.hasApiKey) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("DashScope API Key（可选覆盖）") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveApiKey(apiKey) },
                    enabled = apiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存 Key 到加密存储") }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = qwenModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("记餐/运动文本模型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        listOf("qwen-plus", "qwen-turbo", "qwen-max").forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    qwenModel = it
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    "助手聊天固定使用 Qwen-Max。请尽快轮换曾暴露的 Key。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionTitle("适配说明")
                Text(
                    "已启用边到边布局与系统栏 Insets，跟随系统深浅色与字体缩放，适配 vivo X200 Pro / OriginOS 6 手势导航。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

/** 展示用：去掉多余尾零，避免 75.0 与输入不一致的观感。 */
private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return ""
    val asInt = value.roundToInt()
    return if (kotlin.math.abs(value - asInt) < 1e-9) {
        asInt.toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

private fun filterIntText(raw: String): String =
    raw.filter { it.isDigit() }.take(3)

private fun filterDecimalText(raw: String): String {
    val normalized = raw.replace(',', '.')
    val sb = StringBuilder()
    var dot = false
    normalized.forEach { c ->
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && !dot -> {
                sb.append(c)
                dot = true
            }
        }
    }
    return sb.toString().take(8)
}

private fun parseProfileDraft(
    base: UserProfileEntity,
    name: String,
    sex: Sex,
    ageText: String,
    heightText: String,
    weightText: String,
    targetWeightText: String,
    kgPerWeekText: String,
    proteinText: String,
    activityLevel: ActivityLevel,
    qwenModel: String
): UserProfileEntity? {
    val age = ageText.toIntOrNull()?.takeIf { it in 10..100 } ?: return null
    val height = heightText.toDoubleOrNull()?.takeIf { it in 100.0..250.0 } ?: return null
    val weight = weightText.toDoubleOrNull()?.takeIf { it in 30.0..300.0 } ?: return null
    val target = targetWeightText.toDoubleOrNull()?.takeIf { it in 30.0..300.0 } ?: return null
    val kgWeek = kgPerWeekText.toDoubleOrNull()?.takeIf { it in 0.1..1.0 } ?: return null
    val protein = proteinText.toDoubleOrNull()?.takeIf { it in 0.8..3.5 } ?: return null
    return base.copy(
        name = name.trim().ifBlank { base.name },
        sex = sex,
        age = age,
        heightCm = height,
        weightKg = weight,
        targetWeightKg = target,
        kgPerWeek = kgWeek,
        proteinPerKg = protein,
        activityLevel = activityLevel,
        qwenModel = qwenModel
    )
}
