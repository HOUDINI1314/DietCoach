package com.dietcoach.app.domain

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val factor: Double, val labelZh: String) {
    SEDENTARY(1.2, "久坐"),
    LIGHT(1.375, "轻度活动"),
    MODERATE(1.55, "中度活动"),
    ACTIVE(1.725, "高强度"),
    VERY_ACTIVE(1.9, "非常高强度")
}

enum class MealType(val labelZh: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐")
}

enum class EntrySource { MANUAL, AI, VLM, PHOTO }

enum class WorkoutIntensity(val met: Double, val labelZh: String) {
    LOW(3.5, "低"),
    MEDIUM(6.0, "中"),
    HIGH(8.5, "高")
}

enum class StrengthCategory(val labelZh: String) {
    PUSH("推"),
    PULL("拉"),
    LEGS("腿"),
    CORE("核心"),
    FULL("全身"),
    OTHER("其他")
}

data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbG: Int,
    val fatG: Int
)

data class DayTotals(
    val intakeKcal: Int = 0,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val burnKcal: Int = 0,
    val extraBurnKcal: Int = 0,
    val strengthKcal: Int = 0
) {
    val totalBurn: Int get() = burnKcal + extraBurnKcal + strengthKcal
}

data class DayStats(
    val date: String,
    val totals: DayTotals,
    val bmr: Int,
    val tdee: Int,
    val target: MacroTargets,
    val deficit: Int
)

data class ChatUiMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val content: String,
    val pending: Boolean = false
)

object StrengthCatalog {
    val exercises: Map<StrengthCategory, List<String>> = mapOf(
        StrengthCategory.PUSH to listOf("杠铃卧推", "哑铃卧推", "上斜卧推", "俯卧撑", "肩推", "侧平举", "臂屈伸"),
        StrengthCategory.PULL to listOf("引体向上", "高位下拉", "杠铃划船", "哑铃划船", "面拉", "二头弯举"),
        StrengthCategory.LEGS to listOf("深蹲", "腿举", "罗马尼亚硬拉", "弓步蹲", "腿弯举", "提踵"),
        StrengthCategory.CORE to listOf("卷腹", "悬垂举腿", "平板支撑", "俄罗斯转体", "龙旗"),
        StrengthCategory.FULL to listOf("硬拉", "高翻", "波比跳", "农夫行走"),
        StrengthCategory.OTHER to listOf("自定义动作")
    )

    /** 粗估：容量(kg) * 0.05 + 时长相关 */
    fun estimateKcal(volumeKg: Double, minutes: Int, bodyWeightKg: Double): Int {
        val fromVolume = volumeKg * 0.05
        val fromTime = bodyWeightKg * 0.07 * (minutes / 60.0).coerceAtLeast(0.15)
        return (fromVolume + fromTime).toInt().coerceAtLeast(1)
    }
}
