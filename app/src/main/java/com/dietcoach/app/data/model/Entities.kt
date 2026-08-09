package com.dietcoach.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dietcoach.app.domain.ActivityLevel
import com.dietcoach.app.domain.EntrySource
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.domain.Sex
import com.dietcoach.app.domain.StrengthCategory
import com.dietcoach.app.domain.WorkoutIntensity

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "我",
    val sex: Sex = Sex.MALE,
    val age: Int = 28,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val kgPerWeek: Double = 0.5,
    val proteinPerKg: Double = 1.8,
    val fatCalorieRatio: Double = 0.28,
    val qwenModel: String = "qwen-plus",
    val vlmModel: String = "qwen-vl-plus",
    val onboardingDone: Boolean = false
)

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val mealType: MealType,
    val name: String,
    val amount: String,
    val kcal: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val source: EntrySource = EntrySource.MANUAL,
    val rawText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_entries")
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String,
    val minutes: Int,
    val intensity: WorkoutIntensity,
    val kcal: Int,
    val source: EntrySource = EntrySource.MANUAL,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "strength_entries")
data class StrengthEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: StrengthCategory,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val loadKg: Double,
    val volumeKg: Double,
    val minutes: Int,
    val kcal: Int,
    val note: String = "",
    val source: EntrySource = EntrySource.MANUAL,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey val date: String,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_extras")
data class DailyExtraEntity(
    @PrimaryKey val date: String,
    val extraBurnKcal: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
