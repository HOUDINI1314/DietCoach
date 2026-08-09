package com.dietcoach.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dietcoach.app.data.model.ChatMessageEntity
import com.dietcoach.app.data.model.DailyExtraEntity
import com.dietcoach.app.data.model.FoodEntryEntity
import com.dietcoach.app.data.model.StrengthEntryEntity
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.data.model.WeightLogEntity
import com.dietcoach.app.data.model.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY createdAt ASC")
    fun observeByDate(date: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT DISTINCT date FROM food_entries")
    fun observeDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY createdAt ASC")
    suspend fun getByDate(date: String): List<FoodEntryEntity>

    @Insert
    suspend fun insert(entry: FoodEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<FoodEntryEntity>)

    @Update
    suspend fun update(entry: FoodEntryEntity)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WorkoutEntryDao {
    @Query("SELECT * FROM workout_entries WHERE date = :date ORDER BY createdAt ASC")
    fun observeByDate(date: String): Flow<List<WorkoutEntryEntity>>

    @Query("SELECT DISTINCT date FROM workout_entries")
    fun observeDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM workout_entries WHERE date = :date ORDER BY createdAt ASC")
    suspend fun getByDate(date: String): List<WorkoutEntryEntity>

    @Insert
    suspend fun insert(entry: WorkoutEntryEntity): Long

    @Update
    suspend fun update(entry: WorkoutEntryEntity)

    @Query("DELETE FROM workout_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface StrengthEntryDao {
    @Query("SELECT * FROM strength_entries WHERE date = :date ORDER BY createdAt ASC")
    fun observeByDate(date: String): Flow<List<StrengthEntryEntity>>

    @Query("SELECT DISTINCT date FROM strength_entries")
    fun observeDistinctDates(): Flow<List<String>>

    @Insert
    suspend fun insert(entry: StrengthEntryEntity): Long

    @Query("DELETE FROM strength_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY date ASC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE date = :date")
    suspend fun get(date: String): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE date = :date")
    fun observeDate(date: String): Flow<WeightLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: WeightLogEntity)
}

@Dao
interface DailyExtraDao {
    @Query("SELECT * FROM daily_extras WHERE date = :date")
    fun observe(date: String): Flow<DailyExtraEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(extra: DailyExtraEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clear()
}
