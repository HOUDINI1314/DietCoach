package com.dietcoach.app.domain

import kotlin.math.max
import kotlin.math.roundToInt

object NutritionCalculator {

    /** Mifflin-St Jeor BMR */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    /** 仅作参考展示；热量缺口不再乘活动系数。 */
    fun tdee(bmr: Double, activity: ActivityLevel): Double = bmr * activity.factor

    /**
     * 1 kg fat ≈ 7700 kcal. Weekly loss rate → daily planned deficit.
     */
    fun plannedDailyDeficit(kgPerWeek: Double): Int {
        val raw = (kgPerWeek * 7700.0 / 7.0).roundToInt()
        return raw.coerceIn(0, 1000)
    }

    /** 目标摄入以 BMR 为基底，不乘活动系数。 */
    fun targetCalories(bmr: Double, kgPerWeek: Double, sex: Sex): Int {
        val deficit = plannedDailyDeficit(kgPerWeek)
        val floor = if (sex == Sex.MALE) 1500 else 1200
        return max(floor, (bmr - deficit).roundToInt())
    }

    fun macroTargets(
        weightKg: Double,
        targetCalories: Int,
        proteinPerKg: Double = 1.8,
        fatCalorieRatio: Double = 0.28
    ): MacroTargets {
        val proteinG = (weightKg * proteinPerKg).roundToInt().coerceAtLeast(50)
        val fatG = ((targetCalories * fatCalorieRatio) / 9.0).roundToInt().coerceAtLeast(30)
        val proteinKcal = proteinG * 4
        val fatKcal = fatG * 9
        val carbG = max(0, (targetCalories - proteinKcal - fatKcal) / 4)
        return MacroTargets(
            calories = targetCalories,
            proteinG = proteinG,
            carbG = carbG,
            fatG = fatG
        )
    }

    /**
     * 当日热量缺口 = BMR + 训练消耗（有氧+力量+额外）− 饮食摄入。
     * 正数 = 赤字（偏减脂），负数 = 盈余。不使用活动系数。
     */
    fun deficit(bmr: Int, burn: Int, intake: Int): Int = bmr + burn - intake

    fun estimateWorkoutKcal(
        weightKg: Double,
        minutes: Int,
        intensity: WorkoutIntensity
    ): Int {
        // kcal ≈ MET * weightKg * hours
        return (intensity.met * weightKg * (minutes / 60.0)).roundToInt().coerceAtLeast(0)
    }

    fun buildDayStats(
        date: String,
        sex: Sex,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        activity: ActivityLevel,
        kgPerWeek: Double,
        proteinPerKg: Double,
        fatCalorieRatio: Double,
        totals: DayTotals
    ): DayStats {
        val bmrVal = bmr(sex, weightKg, heightCm, age).roundToInt()
        val tdeeVal = tdee(bmrVal.toDouble(), activity).roundToInt()
        val targetKcal = targetCalories(bmrVal.toDouble(), kgPerWeek, sex)
        val target = macroTargets(weightKg, targetKcal, proteinPerKg, fatCalorieRatio)
        val deficitVal = deficit(bmrVal, totals.totalBurn, totals.intakeKcal)
        return DayStats(
            date = date,
            totals = totals,
            bmr = bmrVal,
            tdee = tdeeVal,
            target = target,
            deficit = deficitVal
        )
    }
}
