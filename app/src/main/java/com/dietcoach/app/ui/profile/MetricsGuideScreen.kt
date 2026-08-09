package com.dietcoach.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dietcoach.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsGuideScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("指标与算法说明") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "以下为本机计算与 AI 提示词对齐的规则说明，数值为估算，不能替代医疗建议。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionTitle("当日体重如何影响一切")
                GuideBody(
                    "BMR、TDEE、本地运动热量、饮食/运动/VLM/聊天的 AI 提示词，都使用「有效体重」：\n" +
                        "• 若当日有称重记录 → 用当日体重\n" +
                        "• 若当日没有记录 → 用「我的」里的当前体重\n" +
                        "修改当日体重或个人资料体重后，后续 AI 调用会立刻按新体重生成提示词。"
                )
            }

            item {
                SectionTitle("BMR（基础代谢）")
                GuideBody(
                    "采用 Mifflin–St Jeor 公式：\n" +
                        "男：BMR = 10×体重(kg) + 6.25×身高(cm) − 5×年龄 + 5\n" +
                        "女：BMR = 10×体重(kg) + 6.25×身高(cm) − 5×年龄 − 161\n" +
                        "含义：安静卧床约一天所需热量，不含日常走动与训练。"
                )
            }

            item {
                SectionTitle("活动系数（仅参考，不计入缺口）")
                GuideBody(
                    "「我的」里的活动水平仍会生成参考 TDEE = BMR × 系数，并写入 AI 提示词，" +
                        "但热量缺口与目标摄入不再乘活动系数，避免与「已记录的训练消耗」重复计算。\n" +
                        "系数含义：久坐 1.2 / 轻度 1.375 / 中度 1.55 / 高强度 1.725 / 非常高强度 1.9。"
                )
            }

            item {
                SectionTitle("目标热量与热量缺口")
                GuideBody(
                    "计划日缺口 ≈ 每周目标减重(kg) × 7700 ÷ 7，限制在 0–1000 kcal。\n" +
                        "目标摄入 ≈ max(性别下限, BMR − 计划缺口)；男下限约 1500、女约 1200 kcal。\n" +
                        "当日缺口 = BMR + 有氧消耗 + 力量消耗 + 额外消耗 − 饮食摄入。\n" +
                        "正数表示当天仍处于热量赤字（偏减脂方向），负数表示盈余。"
                )
            }

            item {
                SectionTitle("三大营养素")
                GuideBody(
                    "默认：蛋白质 ≈ 体重(kg) × 每公斤蛋白目标（默认 1.8g，可在「我的」改）；\n" +
                        "脂肪约占目标热量的 28%；剩余热量分配给碳水。\n" +
                        "可按训练日/休息日自行微调，应用只给基准建议。"
                )
            }

            item {
                SectionTitle("有氧强度（MET）")
                GuideBody(
                    "本地估算：消耗 ≈ MET × 体重(kg) × 时长(小时)。\n" +
                        "• 低强度 MET≈3.5：快走、轻松骑行\n" +
                        "• 中强度 MET≈6.0：慢跑、游泳、球类\n" +
                        "• 高强度 MET≈8.5：间歇跑、高强度课\n" +
                        "用 AI 分析运动时，也会按当日有效体重估算，确认后入库。"
                )
            }

            item {
                SectionTitle("力量训练消耗")
                GuideBody(
                    "本地粗估：容量(组×次×kg)×0.05 + 体重×0.07×时长系数。\n" +
                        "勾选 AI 估算时，提示词同样写入当日有效体重。力量记录会出现在日历绿点与日详情中。"
                )
            }

            item {
                SectionTitle("饮食 AI / 拍照 VLM")
                GuideBody(
                    "文字记餐用文本模型；拍照识餐用视觉模型（默认 qwen-vl-plus）。\n" +
                        "两者系统提示都带上当日有效体重与目标体重，结果需你确认后再入库。\n" +
                        "照片仅用于当次请求，不另存云端相册（取决于你使用的 API 服务商策略）。"
                )
            }

            item {
                SectionTitle("助手聊天")
                GuideBody(
                    "助手固定使用 Qwen-Max，可回答任意问题（不限饮食训练）。涉及健身时会参考当日有效体重。" +
                        "明确说「帮我记录」时，可能附带结构化 JSON 自动写入有氧/力量；日常问答不会擅自入库。" +
                        "记餐/运动解析仍使用「我的」里配置的文本模型（默认 qwen-plus）。"
                )
            }

            item {
                SectionTitle("隐私与 Key")
                GuideBody(
                    "饮食、体重、训练默认存本机 Room 数据库。DashScope API Key 仅保存在本机加密偏好区；分享给他人的安装包不应内置你的 Key。"
                )
            }

            item {
                Text(
                    "版本说明会随算法调整更新；若与界面数字不一致，以当前代码中的计算公式为准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun GuideBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
