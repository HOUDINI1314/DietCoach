package com.dietcoach.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dietcoach.app.data.model.ChatMessageEntity
import com.dietcoach.app.data.model.DailyExtraEntity
import com.dietcoach.app.data.model.FoodEntryEntity
import com.dietcoach.app.data.model.StrengthEntryEntity
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.data.model.WeightLogEntity
import com.dietcoach.app.data.model.WorkoutEntryEntity
import com.dietcoach.app.domain.ActivityLevel
import com.dietcoach.app.domain.EntrySource
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.domain.Sex
import com.dietcoach.app.domain.StrengthCategory
import com.dietcoach.app.domain.WorkoutIntensity

class Converters {
    @TypeConverter fun sexToString(v: Sex): String = v.name
    @TypeConverter fun stringToSex(v: String): Sex = Sex.valueOf(v)

    @TypeConverter fun activityToString(v: ActivityLevel): String = v.name
    @TypeConverter fun stringToActivity(v: String): ActivityLevel = ActivityLevel.valueOf(v)

    @TypeConverter fun mealToString(v: MealType): String = v.name
    @TypeConverter fun stringToMeal(v: String): MealType = MealType.valueOf(v)

    @TypeConverter fun sourceToString(v: EntrySource): String = v.name
    @TypeConverter fun stringToSource(v: String): EntrySource = runCatching { EntrySource.valueOf(v) }.getOrDefault(EntrySource.MANUAL)

    @TypeConverter fun intensityToString(v: WorkoutIntensity): String = v.name
    @TypeConverter fun stringToIntensity(v: String): WorkoutIntensity = WorkoutIntensity.valueOf(v)

    @TypeConverter fun strengthToString(v: StrengthCategory): String = v.name
    @TypeConverter fun stringToStrength(v: String): StrengthCategory = StrengthCategory.valueOf(v)
}

@Database(
    entities = [
        UserProfileEntity::class,
        FoodEntryEntity::class,
        WorkoutEntryEntity::class,
        StrengthEntryEntity::class,
        WeightLogEntity::class,
        DailyExtraEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun workoutEntryDao(): WorkoutEntryDao
    abstract fun strengthEntryDao(): StrengthEntryDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun dailyExtraDao(): DailyExtraDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dietcoach.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
