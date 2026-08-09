package com.dietcoach.app

import com.dietcoach.app.domain.ActivityLevel
import com.dietcoach.app.domain.DayTotals
import com.dietcoach.app.domain.NutritionCalculator
import com.dietcoach.app.domain.Sex
import com.dietcoach.app.domain.WorkoutIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun mifflinStJeor_male() {
        val bmr = NutritionCalculator.bmr(Sex.MALE, 75.0, 175.0, 28)
        // 10*75 + 6.25*175 - 5*28 + 5 = 750 + 1093.75 - 140 + 5 = 1708.75
        assertEquals(1708.75, bmr, 0.01)
    }

    @Test
    fun deficit_usesBmrPlusBurnMinusIntake() {
        // 1700 + 300 - 1800 = 200
        val deficit = NutritionCalculator.deficit(bmr = 1700, burn = 300, intake = 1800)
        assertEquals(200, deficit)
    }

    @Test
    fun deficit_ignoresActivityFactor() {
        val bmr = 1700
        val withBurn = NutritionCalculator.deficit(bmr, burn = 400, intake = 1600)
        assertEquals(500, withBurn)
        // 即使 TDEE 会更大，缺口仍只看 BMR
        val tdee = NutritionCalculator.tdee(bmr.toDouble(), ActivityLevel.VERY_ACTIVE).toInt()
        assertTrue(tdee > bmr)
        assertEquals(bmr + 400 - 1600, withBurn)
    }

    @Test
    fun targetCalories_basedOnBmrRespectsFloor() {
        val low = NutritionCalculator.targetCalories(bmr = 1400.0, kgPerWeek = 1.0, sex = Sex.FEMALE)
        assertTrue(low >= 1200)
        val male = NutritionCalculator.targetCalories(bmr = 1600.0, kgPerWeek = 1.0, sex = Sex.MALE)
        assertTrue(male >= 1500)
    }

    @Test
    fun workoutEstimate_scalesWithTime() {
        val a = NutritionCalculator.estimateWorkoutKcal(70.0, 30, WorkoutIntensity.MEDIUM)
        val b = NutritionCalculator.estimateWorkoutKcal(70.0, 60, WorkoutIntensity.MEDIUM)
        assertTrue(b > a)
    }

    @Test
    fun buildDayStats_deficitUsesBmrNotTdee() {
        val totals = DayTotals(
            intakeKcal = 1600,
            proteinG = 120.0,
            carbG = 150.0,
            fatG = 50.0,
            burnKcal = 200,
            strengthKcal = 100
        )
        val stats = NutritionCalculator.buildDayStats(
            date = "2026-08-09",
            sex = Sex.MALE,
            age = 28,
            heightCm = 175.0,
            weightKg = 75.0,
            activity = ActivityLevel.VERY_ACTIVE,
            kgPerWeek = 0.5,
            proteinPerKg = 1.8,
            fatCalorieRatio = 0.28,
            totals = totals
        )
        assertTrue(stats.tdee > stats.bmr)
        // 缺口 = BMR + 300 - 1600，而不是 TDEE + 300 - 1600
        assertEquals(stats.bmr + totals.totalBurn - totals.intakeKcal, stats.deficit)
        assertTrue(stats.target.proteinG >= 50)
    }
}
