package com.dietcoach.app.data.repo

import com.dietcoach.app.BuildConfig
import com.dietcoach.app.ai.AiParsers
import com.dietcoach.app.ai.ChatMessage
import com.dietcoach.app.ai.ChatRecordBundle
import com.dietcoach.app.ai.ChatRecordItem
import com.dietcoach.app.ai.ChatSendResult
import com.dietcoach.app.ai.DashScopeClient
import com.dietcoach.app.ai.FoodItemParse
import com.dietcoach.app.ai.FoodParseResult
import com.dietcoach.app.ai.Prompts
import com.dietcoach.app.ai.WorkoutBurnParse
import com.dietcoach.app.data.db.AppDatabase
import com.dietcoach.app.data.model.ChatMessageEntity
import com.dietcoach.app.data.model.DailyExtraEntity
import com.dietcoach.app.data.model.FoodEntryEntity
import com.dietcoach.app.data.model.StrengthEntryEntity
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.data.model.WeightLogEntity
import com.dietcoach.app.data.model.WorkoutEntryEntity
import com.dietcoach.app.domain.DayStats
import com.dietcoach.app.domain.DayTotals
import com.dietcoach.app.domain.EntrySource
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.domain.NutritionCalculator
import com.dietcoach.app.domain.StrengthCatalog
import com.dietcoach.app.domain.StrengthCategory
import com.dietcoach.app.domain.WorkoutIntensity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DietRepository(
    private val db: AppDatabase,
    private val ai: DashScopeClient
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().format(dateFmt)

    fun observeProfile(): Flow<UserProfileEntity> =
        db.userProfileDao().observe().map { it ?: UserProfileEntity() }

    suspend fun getProfile(): UserProfileEntity =
        db.userProfileDao().get() ?: UserProfileEntity()

    /** 当日有称重用当日体重，否则用个人资料当前体重。 */
    suspend fun resolveEffectiveWeight(date: String): EffectiveWeight {
        val profile = getProfile()
        val day = db.weightLogDao().get(date)
        return if (day != null && day.weightKg > 0) {
            EffectiveWeight(day.weightKg, fromDayLog = true, date = date)
        } else {
            EffectiveWeight(profile.weightKg, fromDayLog = false, date = date)
        }
    }

    data class EffectiveWeight(
        val kg: Double,
        val fromDayLog: Boolean,
        val date: String
    )

    suspend fun saveProfile(profile: UserProfileEntity) {
        db.userProfileDao().upsert(profile)
        db.weightLogDao().upsert(
            WeightLogEntity(date = today(), weightKg = profile.weightKg)
        )
    }

    fun observeFoods(date: String): Flow<List<FoodEntryEntity>> =
        db.foodEntryDao().observeByDate(date)

    fun observeWorkouts(date: String): Flow<List<WorkoutEntryEntity>> =
        db.workoutEntryDao().observeByDate(date)

    fun observeStrength(date: String): Flow<List<StrengthEntryEntity>> =
        db.strengthEntryDao().observeByDate(date)

    /** 近 N 天体重（按日期升序），供趋势图与日历用，避免全表刷 UI。 */
    fun observeRecentWeights(limit: Int = 90): Flow<List<WeightLogEntity>> =
        db.weightLogDao().observeRecent(limit).map { list -> list.sortedBy { it.date } }

    fun observeChat(): Flow<List<ChatMessageEntity>> =
        db.chatMessageDao().observeAll()

    fun observeActiveDates(): Flow<Set<String>> =
        combine(
            db.foodEntryDao().observeDistinctDates(),
            db.workoutEntryDao().observeDistinctDates(),
            db.strengthEntryDao().observeDistinctDates()
        ) { foods, workouts, strength ->
            (foods + workouts + strength).toSet()
        }

    data class DayBundle(
        val profile: UserProfileEntity,
        val stats: DayStats,
        val foods: List<FoodEntryEntity>,
        val workouts: List<WorkoutEntryEntity>,
        val strength: List<StrengthEntryEntity>
    )

    /** 单日数据只订一次，避免 foods/stats 重复订阅造成卡顿。 */
    fun observeDayBundle(date: String): Flow<DayBundle> {
        return combine(
            observeProfile(),
            db.foodEntryDao().observeByDate(date),
            db.workoutEntryDao().observeByDate(date),
            db.strengthEntryDao().observeByDate(date),
            db.dailyExtraDao().observe(date)
        ) { profile, foods, workouts, strength, extra ->
            DayParts(profile, foods, workouts, strength, extra?.extraBurnKcal ?: 0)
        }.combine(db.weightLogDao().observeDate(date)) { parts, dayWeight ->
            val totals = DayTotals(
                intakeKcal = parts.foods.sumOf { it.kcal },
                proteinG = parts.foods.sumOf { it.proteinG },
                carbG = parts.foods.sumOf { it.carbG },
                fatG = parts.foods.sumOf { it.fatG },
                burnKcal = parts.workouts.sumOf { it.kcal },
                extraBurnKcal = parts.extraBurn,
                strengthKcal = parts.strength.sumOf { it.kcal }
            )
            val stats = NutritionCalculator.buildDayStats(
                date = date,
                sex = parts.profile.sex,
                age = parts.profile.age,
                heightCm = parts.profile.heightCm,
                weightKg = dayWeight?.weightKg ?: parts.profile.weightKg,
                activity = parts.profile.activityLevel,
                kgPerWeek = parts.profile.kgPerWeek,
                proteinPerKg = parts.profile.proteinPerKg,
                fatCalorieRatio = parts.profile.fatCalorieRatio,
                totals = totals
            )
            DayBundle(parts.profile, stats, parts.foods, parts.workouts, parts.strength)
        }
    }

    private data class DayParts(
        val profile: UserProfileEntity,
        val foods: List<FoodEntryEntity>,
        val workouts: List<WorkoutEntryEntity>,
        val strength: List<StrengthEntryEntity>,
        val extraBurn: Int
    )

    suspend fun addFood(entry: FoodEntryEntity) {
        db.foodEntryDao().insert(entry)
    }

    suspend fun addFoods(entries: List<FoodEntryEntity>) {
        db.foodEntryDao().insertAll(entries)
    }

    suspend fun deleteFood(id: Long) = db.foodEntryDao().delete(id)

    suspend fun addWorkout(
        date: String,
        name: String,
        minutes: Int,
        intensity: WorkoutIntensity,
        kcal: Int? = null,
        source: EntrySource = EntrySource.MANUAL
    ) {
        val weight = resolveEffectiveWeight(date)
        val resolved = kcal ?: NutritionCalculator.estimateWorkoutKcal(weight.kg, minutes, intensity)
        db.workoutEntryDao().insert(
            WorkoutEntryEntity(
                date = date,
                name = name,
                minutes = minutes,
                intensity = intensity,
                kcal = resolved,
                source = source
            )
        )
    }

    suspend fun deleteWorkout(id: Long) = db.workoutEntryDao().delete(id)

    suspend fun addStrength(
        date: String,
        category: StrengthCategory,
        exerciseName: String,
        sets: Int,
        reps: Int,
        loadKg: Double,
        minutes: Int,
        kcalOverride: Int? = null,
        note: String = "",
        source: EntrySource = EntrySource.MANUAL,
        useAiKcal: Boolean = false
    ) {
        val profile = getProfile()
        val weight = resolveEffectiveWeight(date)
        val volume = sets * reps * loadKg
        val localKcal = StrengthCatalog.estimateKcal(volume, minutes, weight.kg)
        val kcal = when {
            kcalOverride != null -> kcalOverride
            useAiKcal -> {
                val desc = "动作=$exerciseName 分类=${category.labelZh} 组数=$sets 次数=$reps 负荷=${loadKg}kg 时长=${minutes}分钟 容量=${volume}kg"
                val aiKcal = ai.estimateStrengthBurn(
                    profile.qwenModel,
                    profile,
                    desc,
                    weight.kg,
                    weight.date,
                    weight.fromDayLog
                )
                if (aiKcal > 0) aiKcal else localKcal
            }
            else -> localKcal
        }
        db.strengthEntryDao().insert(
            StrengthEntryEntity(
                date = date,
                category = category,
                exerciseName = exerciseName,
                sets = sets,
                reps = reps,
                loadKg = loadKg,
                volumeKg = volume,
                minutes = minutes,
                kcal = kcal,
                note = note,
                source = source
            )
        )
    }

    suspend fun deleteStrength(id: Long) = db.strengthEntryDao().delete(id)

    suspend fun setExtraBurn(date: String, kcal: Int) {
        db.dailyExtraDao().upsert(DailyExtraEntity(date = date, extraBurnKcal = kcal))
    }

    suspend fun logWeight(date: String, weightKg: Double) {
        db.weightLogDao().upsert(WeightLogEntity(date = date, weightKg = weightKg))
        if (date == today()) {
            val profile = getProfile()
            db.userProfileDao().upsert(profile.copy(weightKg = weightKg))
        }
    }

    suspend fun parseFoodWithAi(date: String, utterance: String): FoodParseResult {
        val profile = getProfile()
        val weight = resolveEffectiveWeight(date)
        return ai.parseFood(
            profile.qwenModel,
            profile,
            utterance,
            weight.kg,
            weight.date,
            weight.fromDayLog
        )
    }

    suspend fun parseFoodFromPhoto(
        date: String,
        bytes: ByteArray,
        mime: String = "image/jpeg"
    ): FoodParseResult {
        val profile = getProfile()
        val weight = resolveEffectiveWeight(date)
        return ai.parseFoodFromImage(
            vlmModel = profile.vlmModel,
            profile = profile,
            imageBytes = bytes,
            mime = mime,
            effectiveWeightKg = weight.kg,
            date = weight.date,
            weightFromDayLog = weight.fromDayLog
        )
    }

    suspend fun confirmAiFoods(
        date: String,
        mealType: MealType,
        items: List<FoodItemParse>,
        rawText: String,
        source: EntrySource = EntrySource.AI
    ) {
        val entries = items.map {
            FoodEntryEntity(
                date = date,
                mealType = mealType,
                name = it.name,
                amount = it.amount,
                kcal = it.kcal,
                proteinG = it.proteinG,
                carbG = it.carbG,
                fatG = it.fatG,
                source = source,
                rawText = rawText
            )
        }
        addFoods(entries)
    }

    suspend fun analyzeWorkoutAndSave(date: String, description: String): WorkoutBurnParse {
        val profile = getProfile()
        val weight = resolveEffectiveWeight(date)
        val parsed = ai.estimateWorkoutBurn(
            profile.qwenModel,
            profile,
            description,
            weight.kg,
            weight.date,
            weight.fromDayLog
        )
        val intensity = runCatching { WorkoutIntensity.valueOf(parsed.intensity.uppercase()) }
            .getOrDefault(WorkoutIntensity.MEDIUM)
        addWorkout(
            date = date,
            name = parsed.name.ifBlank { "AI运动" },
            minutes = parsed.minutes.coerceAtLeast(1),
            intensity = intensity,
            kcal = parsed.kcal.coerceAtLeast(1),
            source = EntrySource.AI
        )
        return parsed
    }

    suspend fun sendChat(
        date: String,
        userText: String,
        onPartial: suspend (String) -> Unit = {}
    ): ChatSendResult {
        val profile = getProfile()
        val weight = resolveEffectiveWeight(date)
        db.chatMessageDao().insert(ChatMessageEntity(role = "user", content = userText))
        // 用 suspend 查询，避免 Flow.first() 读到未包含刚插入用户消息的旧快照
        val hist = db.chatMessageDao().getAll()
            .takeLast(20)
            .map { ChatMessage(it.role, it.content) }
        val system = Prompts.chatSystem(profile, weight.kg, weight.date, weight.fromDayLog)
        val model = BuildConfig.CHAT_QWEN_MODEL.ifBlank { "qwen-max" }
        val acc = StringBuilder()
        runCatching {
            ai.chatWithHistoryStream(model, system, hist).collect { delta ->
                acc.append(delta)
                onPartial(acc.toString())
            }
        }.onFailure { err ->
            // 流式失败时回退非流式，避免整句丢失
            if (acc.isEmpty()) {
                acc.append(ai.chatWithHistory(model, system, hist))
                onPartial(acc.toString())
            } else {
                throw err
            }
        }
        val rawReply = acc.toString().ifBlank { error("模型未返回内容") }

        val labels = mutableListOf<String>()
        val seen = linkedSetOf<String>()

        if (AiParsers.wantsRecord(userText)) {
            // 1) 饮食专用解析：擅长「汉堡+鸡翅」等多条目拆分
            if (AiParsers.looksLikeFoodRecord(userText)) {
                runCatching {
                    val foods = parseFoodWithAi(date, userText)
                    val meal = AiParsers.mealHintToType(foods.mealHint, guessMealFromText(userText))
                    foods.items.forEach { food ->
                        ingestFoodParse(date, food, meal, seen, labels)
                    }
                }
            }
            // 2) 结构化提取：补全遗漏食物 / 训练（始终执行，不再因已有一条就跳过）
            runCatching {
                val extracted = ai.chat(
                    model = BuildConfig.CHAT_QWEN_MODEL.ifBlank { "qwen-max" },
                    system = Prompts.chatRecordExtractSystem(
                        profile, weight.kg, weight.date, weight.fromDayLog
                    ),
                    user = buildString {
                        appendLine("请拆条提取，每种食物单独一条，不要合并。")
                        appendLine("用户说：")
                        appendLine(userText)
                        appendLine()
                        appendLine("助手回复（仅供热量参考）：")
                        appendLine(rawReply)
                    },
                    jsonMode = true
                )
                ingestRecordBundle(
                    date,
                    AiParsers.parseChatRecordBundle(extracted),
                    seen,
                    labels
                )
            }
            // 3) 回复中的标签（支持多个 FOOD_JSON）
            runCatching { ingestTaggedRecords(date, rawReply, seen, labels) }
        } else {
            runCatching { ingestTaggedRecords(date, rawReply, seen, labels) }
        }

        val cleaned = AiParsers.stripRecordTags(rawReply)
        val finalReply = if (labels.isNotEmpty()) {
            "$cleaned\n\n✅ 已入库（${labels.size}条）：${labels.joinToString("、")}"
        } else if (AiParsers.wantsRecord(userText)) {
            "$cleaned\n\n⚠️ 未能自动入库：请分别写出食物名称（如：汉堡、鸡翅）并再说「帮我记录」。"
        } else {
            cleaned
        }
        db.chatMessageDao().insert(ChatMessageEntity(role = "assistant", content = finalReply))
        return ChatSendResult(finalReply, labels)
    }

    private val recordItemAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(ChatRecordItem::class.java)

    private fun guessMealFromText(text: String): MealType = when {
        listOf("早餐", "早饭", "早上").any { text.contains(it) } -> MealType.BREAKFAST
        listOf("午餐", "午饭", "中午").any { text.contains(it) } -> MealType.LUNCH
        listOf("晚餐", "晚饭", "晚上").any { text.contains(it) } -> MealType.DINNER
        else -> MealType.SNACK
    }

    private suspend fun ingestTaggedRecords(
        date: String,
        reply: String,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        AiParsers.extractAllTaggedPairs(reply).forEach { (tag, json) ->
            when (tag) {
                "WORKOUT_JSON" -> {
                    val burn = AiParsers.parseWorkoutBurn(json)
                    ingestWorkout(
                        date, burn.name, burn.minutes, burn.intensity, burn.kcal, seen, labels
                    )
                }
                "STRENGTH_JSON" -> {
                    val item = recordItemAdapter.fromJson(AiParsers.extractJsonObject(json))
                        ?: return@forEach
                    ingestStrengthItem(date, item, seen, labels)
                }
                "FOOD_JSON" -> {
                    AiParsers.parseFoodTagPayload(json).forEach { item ->
                        ingestFoodItem(date, item, seen, labels)
                    }
                }
            }
        }
    }

    private suspend fun ingestRecordBundle(
        date: String,
        bundle: ChatRecordBundle,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        bundle.items.forEach { item ->
            when (item.type.lowercase()) {
                "workout" -> ingestWorkout(
                    date, item.name, item.minutes, item.intensity, item.kcal, seen, labels
                )
                "strength" -> ingestStrengthItem(date, item, seen, labels)
                "food" -> ingestFoodItem(date, item, seen, labels)
            }
        }
    }

    private suspend fun ingestWorkout(
        date: String,
        name: String,
        minutes: Int,
        intensityRaw: String,
        kcal: Int,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        val resolvedName = name.ifBlank { "AI运动" }
        val key = AiParsers.normalizeRecordKey("workout", resolvedName)
        if (!seen.add(key)) return
        val intensity = runCatching { WorkoutIntensity.valueOf(intensityRaw.uppercase()) }
            .getOrDefault(WorkoutIntensity.MEDIUM)
        val resolvedKcal = kcal.coerceAtLeast(1)
        addWorkout(
            date,
            resolvedName,
            minutes.coerceAtLeast(1),
            intensity,
            resolvedKcal,
            EntrySource.AI
        )
        labels += "有氧 $resolvedName ${resolvedKcal}kcal"
    }

    private suspend fun ingestStrengthItem(
        date: String,
        item: ChatRecordItem,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        val name = item.exerciseName.ifBlank { item.name.ifBlank { "力量动作" } }
        val key = AiParsers.normalizeRecordKey("strength", name)
        if (!seen.add(key)) return
        val category = runCatching {
            StrengthCategory.valueOf(item.category.uppercase())
        }.getOrDefault(StrengthCategory.OTHER)
        val kcal = item.kcal.takeIf { it > 0 }
        addStrength(
            date = date,
            category = category,
            exerciseName = name,
            sets = item.sets.coerceAtLeast(1),
            reps = item.reps.coerceAtLeast(1),
            loadKg = item.loadKg.coerceAtLeast(0.0),
            minutes = item.minutes.coerceAtLeast(1),
            kcalOverride = kcal,
            source = EntrySource.AI
        )
        labels += if (kcal != null) "力量 $name ${kcal}kcal" else "力量 $name"
    }

    private suspend fun ingestFoodItem(
        date: String,
        item: ChatRecordItem,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        val food = FoodItemParse(
            name = item.name.ifBlank { "饮食" },
            amount = item.amount.ifBlank { "1份" },
            kcal = item.kcal.coerceAtLeast(1),
            proteinG = item.proteinG,
            carbG = item.carbG,
            fatG = item.fatG
        )
        ingestFoodParse(
            date,
            food,
            AiParsers.mealHintToType(item.mealHint, MealType.SNACK),
            seen,
            labels
        )
    }

    private suspend fun ingestFoodParse(
        date: String,
        food: FoodItemParse,
        mealType: MealType,
        seen: MutableSet<String>,
        labels: MutableList<String>
    ) {
        val name = food.name.ifBlank { "饮食" }
        val key = AiParsers.normalizeRecordKey("food", name)
        if (!seen.add(key)) return
        confirmAiFoods(
            date = date,
            mealType = mealType,
            items = listOf(food.copy(name = name)),
            rawText = "chat:$name",
            source = EntrySource.AI
        )
        labels += "饮食 $name ${food.kcal.coerceAtLeast(1)}kcal"
    }

    suspend fun clearChat() = db.chatMessageDao().clear()

    fun shiftDate(date: String, days: Long): String =
        LocalDate.parse(date, dateFmt).plusDays(days).format(dateFmt)
}
