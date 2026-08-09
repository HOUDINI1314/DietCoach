package com.dietcoach.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dietcoach.app.DietCoachApp
import com.dietcoach.app.ai.AiParsers
import com.dietcoach.app.ai.FoodParseResult
import com.dietcoach.app.data.model.ChatMessageEntity
import com.dietcoach.app.data.model.FoodEntryEntity
import com.dietcoach.app.data.model.StrengthEntryEntity
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.data.model.WeightLogEntity
import com.dietcoach.app.data.model.WorkoutEntryEntity
import com.dietcoach.app.data.repo.DietRepository
import com.dietcoach.app.data.secrets.ApiKeyStore
import com.dietcoach.app.domain.DayStats
import com.dietcoach.app.domain.EntrySource
import com.dietcoach.app.domain.MealType
import com.dietcoach.app.domain.StrengthCategory
import com.dietcoach.app.domain.WorkoutIntensity
import com.dietcoach.app.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiBanner(val message: String, val isError: Boolean = false)

data class AppUiState(
    val selectedDate: String,
    val profile: UserProfileEntity = UserProfileEntity(),
    val stats: DayStats? = null,
    val foods: List<FoodEntryEntity> = emptyList(),
    val workouts: List<WorkoutEntryEntity> = emptyList(),
    val strength: List<StrengthEntryEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val activeDates: Set<String> = emptySet(),
    val aiPreview: FoodParseResult? = null,
    val aiUtterance: String = "",
    val busy: Boolean = false,
    val banner: UiBanner? = null,
    val online: Boolean = true,
    val hasApiKey: Boolean = false
) {
    val weightByDate: Map<String, Double>
        get() = weights.associate { it.date to it.weightKg }

    fun weightOn(date: String): Double? = weightByDate[date]

    /** 当日称重优先，否则个人资料体重。 */
    fun effectiveWeightKg(date: String = selectedDate): Double =
        weightByDate[date] ?: profile.weightKg

    fun hasDayWeight(date: String = selectedDate): Boolean =
        weightByDate.containsKey(date)

    fun effectiveWeightHint(date: String = selectedDate): String {
        val kg = String.format("%.1f", effectiveWeightKg(date))
        return if (hasDayWeight(date)) {
            "当日体重 ${kg}kg（已写入 AI/VLM 提示词）"
        } else {
            "当日未称重，使用个人资料体重 ${kg}kg（已写入 AI/VLM 提示词）"
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    application: Application,
    private val repo: DietRepository,
    private val apiKeyStore: ApiKeyStore
) : AndroidViewModel(application) {

    private val selectedDate = MutableStateFlow(repo.today())
    private val aiPreview = MutableStateFlow<FoodParseResult?>(null)
    private val aiUtterance = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val banner = MutableStateFlow<UiBanner?>(null)
    private val online = MutableStateFlow(NetworkMonitor.isOnline(application))
    private val keyTick = MutableStateFlow(0)
    private val streamingAssistant = MutableStateFlow<String?>(null)

    private val dayCore = selectedDate.flatMapLatest { date ->
        repo.observeDayBundle(date).map { bundle ->
            Core(date, bundle.profile, bundle.stats, bundle.foods, bundle.workouts, bundle.strength)
        }
    }

    /** 聊天单独流，避免助手消息刷新带动今日/记录整页重组。 */
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repo.observeChat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 流式生成中的助手草稿；完成后为 null。 */
    val streamingAssistantText: StateFlow<String?> = streamingAssistant

    val uiState: StateFlow<AppUiState> = combine(
        dayCore,
        repo.observeRecentWeights(90),
        repo.observeActiveDates(),
        combine(aiPreview, aiUtterance, busy, banner, online) { preview, utterance, isBusy, note, isOnline ->
            Side(preview, utterance, isBusy, note, isOnline)
        },
        keyTick
    ) { core, weights, active, side, _ ->
        AppUiState(
            selectedDate = core.date,
            profile = core.profile,
            stats = core.stats,
            foods = core.foods,
            workouts = core.workouts,
            strength = core.strength,
            weights = weights,
            activeDates = active,
            aiPreview = side.preview,
            aiUtterance = side.utterance,
            busy = side.busy,
            banner = side.banner,
            online = side.online,
            hasApiKey = apiKeyStore.hasAnyKey()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppUiState(selectedDate = repo.today(), hasApiKey = apiKeyStore.hasAnyKey())
    )

    fun refreshNetwork() {
        online.value = NetworkMonitor.isOnline(getApplication())
    }

    fun shiftDay(delta: Long) {
        selectedDate.update { repo.shiftDate(it, delta) }
        clearBanner()
    }

    fun goToday() {
        selectedDate.value = repo.today()
    }

    fun selectDate(date: String) {
        selectedDate.value = date
        clearBanner()
    }

    fun clearBanner() {
        banner.value = null
    }

    fun saveProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            runCatching { repo.saveProfile(profile.copy(onboardingDone = true)) }
                .onSuccess { banner.value = UiBanner("画像已保存（VLM 将使用最新体重）") }
                .onFailure { banner.value = UiBanner(it.message ?: "保存失败", true) }
        }
    }

    fun saveApiKey(key: String) {
        apiKeyStore.setApiKey(key)
        keyTick.update { it + 1 }
        banner.value = UiBanner("API Key 已安全保存到本机")
    }

    fun addManualFood(
        mealType: MealType,
        name: String,
        amount: String,
        kcal: Int,
        protein: Double,
        carb: Double,
        fat: Double
    ) {
        viewModelScope.launch {
            repo.addFood(
                FoodEntryEntity(
                    date = selectedDate.value,
                    mealType = mealType,
                    name = name,
                    amount = amount,
                    kcal = kcal,
                    proteinG = protein,
                    carbG = carb,
                    fatG = fat,
                    source = EntrySource.MANUAL
                )
            )
            banner.value = UiBanner("已添加饮食")
        }
    }

    fun deleteFood(id: Long) {
        viewModelScope.launch { repo.deleteFood(id) }
    }

    fun addWorkout(name: String, minutes: Int, intensity: WorkoutIntensity) {
        viewModelScope.launch {
            repo.addWorkout(selectedDate.value, name, minutes, intensity)
            banner.value = UiBanner("已添加训练")
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch { repo.deleteWorkout(id) }
    }

    fun setExtraBurn(kcal: Int) {
        viewModelScope.launch { repo.setExtraBurn(selectedDate.value, kcal) }
    }

    fun logWeight(weight: Double, date: String? = null) {
        viewModelScope.launch {
            val target = date ?: selectedDate.value
            repo.logWeight(target, weight)
            banner.value = UiBanner("体重已记录（$target），AI/VLM 提示词已对齐")
        }
    }

    fun parseFoodNlp(text: String) {
        refreshNetwork()
        if (!online.value) {
            banner.value = UiBanner("当前离线，请改用手动记餐", true)
            return
        }
        if (!apiKeyStore.hasAnyKey()) {
            banner.value = UiBanner("请先在「我的」配置 DashScope Key", true)
            return
        }
        viewModelScope.launch {
            busy.value = true
            aiUtterance.value = text
            runCatching { repo.parseFoodWithAi(selectedDate.value, text) }
                .onSuccess {
                    aiPreview.value = it
                    if (it.items.isEmpty()) banner.value = UiBanner("未识别到食物", true)
                }
                .onFailure { banner.value = UiBanner(it.message ?: "AI 解析失败", true) }
            busy.value = false
        }
    }

    fun parseFoodPhoto(bytes: ByteArray, mime: String = "image/jpeg") {
        refreshNetwork()
        if (!online.value) {
            banner.value = UiBanner("离线无法拍照识餐", true)
            return
        }
        if (!apiKeyStore.hasAnyKey()) {
            banner.value = UiBanner("请先配置 DashScope Key", true)
            return
        }
        viewModelScope.launch {
            busy.value = true
            aiUtterance.value = "[拍照识餐]"
            runCatching { repo.parseFoodFromPhoto(selectedDate.value, bytes, mime) }
                .onSuccess {
                    aiPreview.value = it
                    if (it.items.isEmpty()) banner.value = UiBanner("图片中未识别到食物", true)
                    else banner.value = UiBanner("VLM 已估算，请确认后入库")
                }
                .onFailure { banner.value = UiBanner(it.message ?: "识图失败", true) }
            busy.value = false
        }
    }

    fun confirmAiFoods(mealType: MealType) {
        val preview = aiPreview.value ?: return
        val utterance = aiUtterance.value
        val source = if (utterance.startsWith("[拍照")) EntrySource.VLM else EntrySource.AI
        val resolvedMeal = AiParsers.mealHintToType(preview.mealHint, mealType)
        viewModelScope.launch {
            repo.confirmAiFoods(selectedDate.value, resolvedMeal, preview.items, utterance, source)
            aiPreview.value = null
            aiUtterance.value = ""
            banner.value = UiBanner("记餐已确认入库")
        }
    }

    fun dismissAiPreview() {
        aiPreview.value = null
    }

    fun analyzeWorkoutAi(description: String) {
        refreshNetwork()
        if (!online.value || !apiKeyStore.hasAnyKey()) {
            banner.value = UiBanner("需要联网与 API Key", true)
            return
        }
        viewModelScope.launch {
            busy.value = true
            runCatching { repo.analyzeWorkoutAndSave(selectedDate.value, description) }
                .onSuccess { banner.value = UiBanner("AI 已估算并入库：${it.name} ${it.kcal}kcal") }
                .onFailure { banner.value = UiBanner(it.message ?: "分析失败", true) }
            busy.value = false
        }
    }

    fun addStrength(
        category: StrengthCategory,
        exerciseName: String,
        sets: Int,
        reps: Int,
        loadKg: Double,
        minutes: Int,
        useAiKcal: Boolean
    ) {
        viewModelScope.launch {
            busy.value = useAiKcal
            runCatching {
                repo.addStrength(
                    date = selectedDate.value,
                    category = category,
                    exerciseName = exerciseName,
                    sets = sets,
                    reps = reps,
                    loadKg = loadKg,
                    minutes = minutes,
                    useAiKcal = useAiKcal
                )
            }.onSuccess {
                banner.value = UiBanner("力量训练已记录")
            }.onFailure {
                banner.value = UiBanner(it.message ?: "保存失败", true)
            }
            busy.value = false
        }
    }

    fun deleteStrength(id: Long) {
        viewModelScope.launch { repo.deleteStrength(id) }
    }

    fun sendChat(text: String) {
        refreshNetwork()
        if (!online.value || !apiKeyStore.hasAnyKey()) {
            banner.value = UiBanner("聊天需要联网与 API Key", true)
            return
        }
        viewModelScope.launch {
            busy.value = true
            streamingAssistant.value = ""
            runCatching {
                repo.sendChat(selectedDate.value, text) { partial ->
                    streamingAssistant.value = partial
                }
            }
                .onSuccess { result ->
                    banner.value = when {
                        result.ingestedLabels.isNotEmpty() ->
                            UiBanner("已入库：${result.ingestedLabels.joinToString("、")}")
                        com.dietcoach.app.ai.AiParsers.wantsRecord(text) ->
                            UiBanner("想记录但未入库，请补充具体内容后再说「帮我记录」", true)
                        else -> null
                    }
                }
                .onFailure { banner.value = UiBanner(it.message ?: "发送失败", true) }
            streamingAssistant.value = null
            busy.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch { repo.clearChat() }
    }

    private data class Core(
        val date: String,
        val profile: UserProfileEntity,
        val stats: DayStats,
        val foods: List<FoodEntryEntity>,
        val workouts: List<WorkoutEntryEntity>,
        val strength: List<StrengthEntryEntity> = emptyList()
    )

    private data class Side(
        val preview: FoodParseResult?,
        val utterance: String,
        val busy: Boolean,
        val banner: UiBanner?,
        val online: Boolean
    )

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = (app as DietCoachApp).container
                    return AppViewModel(app, container.repository, container.apiKeyStore) as T
                }
            }
    }
}
