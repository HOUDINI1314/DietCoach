package com.dietcoach.app.ai

import com.dietcoach.app.domain.MealType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class FoodParseResult(
    val items: List<FoodItemParse> = emptyList(),
    val notes: String? = null,
    @Json(name = "meal_hint") val mealHint: String? = null
)

@JsonClass(generateAdapter = true)
data class FoodItemParse(
    val name: String = "",
    val amount: String = "",
    val kcal: Int = 0,
    @Json(name = "protein_g") val proteinG: Double = 0.0,
    @Json(name = "carb_g") val carbG: Double = 0.0,
    @Json(name = "fat_g") val fatG: Double = 0.0,
    val confidence: Double = 0.5
)

@JsonClass(generateAdapter = true)
data class WeekPlanParse(
    val title: String = "本周减脂计划",
    @Json(name = "daily_calorie_range") val dailyCalorieRange: List<Int> = emptyList(),
    @Json(name = "protein_target_g") val proteinTargetG: Int = 0,
    @Json(name = "training_days_per_week") val trainingDaysPerWeek: Int = 0,
    val focus: List<String> = emptyList(),
    val days: List<PlanDay> = emptyList(),
    val warnings: List<String> = emptyList(),
    @Json(name = "summary_markdown") val summaryMarkdown: String = ""
)

@JsonClass(generateAdapter = true)
data class PlanDay(
    val day: String = "",
    @Json(name = "calorie_hint") val calorieHint: Int = 0,
    @Json(name = "meals_idea") val mealsIdea: String = "",
    @Json(name = "workout_idea") val workoutIdea: String = ""
)

@JsonClass(generateAdapter = true)
data class WorkoutBurnParse(
    val name: String = "训练",
    val minutes: Int = 30,
    val intensity: String = "MEDIUM",
    val kcal: Int = 0,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class StrengthKcalParse(
    val kcal: Int = 0,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatRecordBundle(
    val items: List<ChatRecordItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatRecordItem(
    val type: String = "",
    val name: String = "",
    val minutes: Int = 0,
    val intensity: String = "MEDIUM",
    val kcal: Int = 0,
    val category: String = "OTHER",
    @Json(name = "exerciseName") val exerciseName: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    @Json(name = "loadKg") val loadKg: Double = 0.0,
    val amount: String = "",
    @Json(name = "protein_g") val proteinG: Double = 0.0,
    @Json(name = "carb_g") val carbG: Double = 0.0,
    @Json(name = "fat_g") val fatG: Double = 0.0,
    @Json(name = "meal_hint") val mealHint: String? = null
)

data class ChatSendResult(
    val reply: String,
    val ingestedLabels: List<String> = emptyList()
)

object AiParsers {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val foodAdapter = moshi.adapter(FoodParseResult::class.java)
    private val planAdapter = moshi.adapter(WeekPlanParse::class.java)
    private val burnAdapter = moshi.adapter(WorkoutBurnParse::class.java)
    private val strengthKcalAdapter = moshi.adapter(StrengthKcalParse::class.java)
    private val chatRecordAdapter = moshi.adapter(ChatRecordBundle::class.java)

    fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return extractFirstJsonObject(trimmed) ?: trimmed
    }

    /** 提取第一个完整 JSON 对象，避免多标签时 lastIndexOf('}') 吞掉后续内容。 */
    fun extractFirstJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until raw.length) {
            val c = raw[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return raw.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    fun parseFood(raw: String): FoodParseResult {
        val json = extractJsonObject(raw)
        return foodAdapter.fromJson(json) ?: FoodParseResult()
    }

    fun parseWeekPlan(raw: String): WeekPlanParse {
        val json = extractJsonObject(raw)
        return planAdapter.fromJson(json) ?: WeekPlanParse(summaryMarkdown = raw)
    }

    fun parseWorkoutBurn(raw: String): WorkoutBurnParse {
        val json = extractJsonObject(raw)
        return burnAdapter.fromJson(json) ?: WorkoutBurnParse()
    }

    fun parseStrengthKcal(raw: String): Int {
        val json = extractJsonObject(raw)
        return strengthKcalAdapter.fromJson(json)?.kcal?.coerceAtLeast(0) ?: 0
    }

    fun parseChatRecordBundle(raw: String): ChatRecordBundle {
        val json = extractJsonObject(raw)
        return chatRecordAdapter.fromJson(json) ?: ChatRecordBundle()
    }

    fun extractTaggedJson(raw: String, tag: String): String? {
        val marker = Regex("""<<\s*${Regex.escape(tag)}\s*>>""", RegexOption.IGNORE_CASE)
        val match = marker.find(raw) ?: return null
        return extractFirstJsonObject(raw.substring(match.range.last + 1))
    }

    /** 保留全部标签（同一 tag 可出现多次，如两道菜两个 FOOD_JSON）。 */
    fun extractAllTaggedPairs(raw: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val simple = Regex("""<<\s*(WORKOUT_JSON|STRENGTH_JSON|FOOD_JSON)\s*>>""", RegexOption.IGNORE_CASE)
        simple.findAll(raw).forEach { m ->
            val tag = m.groupValues[1].uppercase()
            val json = extractFirstJsonObject(raw.substring(m.range.last + 1))
            if (json != null) out += tag to json
        }
        return out
    }

    @Deprecated("Use extractAllTaggedPairs", ReplaceWith("extractAllTaggedPairs(raw).toMap()"))
    fun extractAllTaggedJson(raw: String): Map<String, String> =
        extractAllTaggedPairs(raw).toMap()

    /**
     * FOOD_JSON 支持：
     * 1) 单条 {"name":"鸡翅",...}
     * 2) 多条 {"items":[{"name":"汉堡",...},{"name":"鸡翅",...}]}
     */
    fun parseFoodTagPayload(json: String, fallbackMealHint: String? = null): List<ChatRecordItem> {
        val obj = extractFirstJsonObject(json) ?: return emptyList()
        if (obj.contains("\"items\"")) {
            val foods = parseFood(obj)
            if (foods.items.isNotEmpty()) {
                val hint = foods.mealHint ?: fallbackMealHint
                return foods.items.map {
                    ChatRecordItem(
                        type = "food",
                        name = it.name,
                        amount = it.amount,
                        kcal = it.kcal,
                        proteinG = it.proteinG,
                        carbG = it.carbG,
                        fatG = it.fatG,
                        mealHint = hint
                    )
                }
            }
            return parseChatRecordBundle(obj).items.map {
                it.copy(type = "food", mealHint = it.mealHint ?: fallbackMealHint)
            }
        }
        val single = moshi.adapter(ChatRecordItem::class.java).fromJson(obj) ?: return emptyList()
        if (single.name.isBlank()) return emptyList()
        return listOf(single.copy(type = "food", mealHint = single.mealHint ?: fallbackMealHint))
    }

    fun stripRecordTags(raw: String): String {
        var text = raw
        val marker = Regex("""<<\s*(WORKOUT_JSON|STRENGTH_JSON|FOOD_JSON)\s*>>""", RegexOption.IGNORE_CASE)
        while (true) {
            val m = marker.find(text) ?: break
            val after = text.substring(m.range.last + 1)
            val json = extractFirstJsonObject(after)
            val end = if (json != null) {
                m.range.last + 1 + after.indexOf(json) + json.length
            } else {
                m.range.last + 1
            }
            text = text.removeRange(m.range.first, end.coerceAtMost(text.length))
        }
        return text.trim().replace(Regex("\n{3,}"), "\n\n")
    }

    fun wantsRecord(userText: String): Boolean {
        val keys = listOf(
            "帮我记", "帮我记录", "记录一下", "记一下", "记上",
            "入库", "保存记录", "记到", "写入", "记入", "记上账"
        )
        return keys.any { userText.contains(it) }
    }

    fun looksLikeFoodRecord(userText: String): Boolean {
        val keys = listOf(
            "吃", "喝", "餐", "早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐",
            "中午", "早上", "晚上", "加餐", "食物", "饮食", "麦当劳", "肯德基",
            "汉堡", "鸡翅", "米饭", "面条", "外卖"
        )
        return keys.any { userText.contains(it) }
    }

    fun normalizeRecordKey(type: String, name: String): String =
        "${type.lowercase()}:${name.trim().lowercase().replace("\\s+".toRegex(), "")}"

    fun mealHintToType(hint: String?, fallback: MealType): MealType {
        return when (hint?.uppercase()) {
            "BREAKFAST" -> MealType.BREAKFAST
            "LUNCH" -> MealType.LUNCH
            "DINNER" -> MealType.DINNER
            "SNACK" -> MealType.SNACK
            else -> fallback
        }
    }

    fun toPrettyMarkdown(plan: WeekPlanParse): String {
        if (plan.summaryMarkdown.isNotBlank()) return plan.summaryMarkdown
        val sb = StringBuilder()
        sb.appendLine("## ${plan.title}")
        if (plan.dailyCalorieRange.size >= 2) {
            sb.appendLine("- 每日热量：${plan.dailyCalorieRange[0]}–${plan.dailyCalorieRange[1]} kcal")
        }
        if (plan.proteinTargetG > 0) sb.appendLine("- 蛋白目标：${plan.proteinTargetG} g")
        if (plan.trainingDaysPerWeek > 0) sb.appendLine("- 训练：每周 ${plan.trainingDaysPerWeek} 天")
        plan.focus.forEach { sb.appendLine("- 重点：$it") }
        plan.days.forEach { day ->
            sb.appendLine()
            sb.appendLine("### ${day.day}")
            if (day.calorieHint > 0) sb.appendLine("- 热量参考：${day.calorieHint} kcal")
            if (day.mealsIdea.isNotBlank()) sb.appendLine("- 饮食：${day.mealsIdea}")
            if (day.workoutIdea.isNotBlank()) sb.appendLine("- 训练：${day.workoutIdea}")
        }
        if (plan.warnings.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("### 注意")
            plan.warnings.forEach { sb.appendLine("- $it") }
        }
        return sb.toString().trim()
    }
}
