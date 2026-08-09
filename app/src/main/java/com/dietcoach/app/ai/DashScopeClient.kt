package com.dietcoach.app.ai

import android.util.Base64
import com.dietcoach.app.BuildConfig
import com.dietcoach.app.data.model.UserProfileEntity
import com.dietcoach.app.data.secrets.ApiKeyStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class DashScopeClient(
    private val apiKeyStore: ApiKeyStore
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val chatRequestAdapter = moshi.adapter(ChatRequest::class.java)
    private val streamChunkAdapter = moshi.adapter(ChatStreamChunk::class.java)

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: DashScopeApi = Retrofit.Builder()
        .baseUrl(BuildConfig.DASHSCOPE_BASE_URL)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DashScopeApi::class.java)

    private fun authHeader(): String {
        val key = apiKeyStore.getApiKey()
        require(key.isNotBlank()) { "未配置 DashScope API Key，请在「我的」中填写或写入 local.properties" }
        return "Bearer $key"
    }

    suspend fun chat(
        model: String,
        system: String,
        user: String,
        jsonMode: Boolean = false
    ): String {
        val response = api.chatCompletions(
            authorization = authHeader(),
            body = ChatRequest(
                model = model.ifBlank { BuildConfig.DEFAULT_QWEN_MODEL },
                messages = listOf(
                    ChatMessage("system", system),
                    ChatMessage("user", user)
                ),
                responseFormat = if (jsonMode) ResponseFormat("json_object") else null
            )
        )
        return response.choices.firstOrNull()?.message?.content
            ?: error("模型未返回内容")
    }

    suspend fun chatWithHistory(
        model: String,
        system: String,
        history: List<ChatMessage>
    ): String {
        val messages = listOf(ChatMessage("system", system)) + history
        val response = api.chatCompletions(
            authorization = authHeader(),
            body = ChatRequest(
                model = model.ifBlank { BuildConfig.DEFAULT_QWEN_MODEL },
                messages = messages,
                temperature = 0.5
            )
        )
        return response.choices.firstOrNull()?.message?.content
            ?: error("模型未返回内容")
    }

    /** OpenAI 兼容 SSE：每次 emit 一段增量 content。 */
    fun chatWithHistoryStream(
        model: String,
        system: String,
        history: List<ChatMessage>
    ): Flow<String> = flow {
        val messages = listOf(ChatMessage("system", system)) + history
        val body = ChatRequest(
            model = model.ifBlank { BuildConfig.DEFAULT_QWEN_MODEL },
            messages = messages,
            temperature = 0.5,
            stream = true
        )
        val json = chatRequestAdapter.toJson(body)
        val request = Request.Builder()
            .url(BuildConfig.DASHSCOPE_BASE_URL.trimEnd('/') + "/chat/completions")
            .header("Authorization", authHeader())
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw IOException("流式请求失败 HTTP ${response.code}: $err")
            }
            val source = response.body?.source() ?: error("流式响应为空")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (data == "[DONE]") break
                val chunk = runCatching { streamChunkAdapter.fromJson(data) }.getOrNull() ?: continue
                val delta = chunk.choices.firstOrNull()?.delta?.content
                if (!delta.isNullOrEmpty()) emit(delta)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun parseFood(
        model: String,
        profile: UserProfileEntity,
        utterance: String,
        effectiveWeightKg: Double,
        date: String,
        weightFromDayLog: Boolean
    ): FoodParseResult {
        val raw = chat(
            model,
            Prompts.foodSystem(profile, effectiveWeightKg, date, weightFromDayLog),
            "请解析以下饮食描述：\n$utterance",
            jsonMode = true
        )
        return AiParsers.parseFood(raw)
    }

    suspend fun parseFoodFromImage(
        vlmModel: String,
        profile: UserProfileEntity,
        imageBytes: ByteArray,
        mime: String = "image/jpeg",
        effectiveWeightKg: Double,
        date: String,
        weightFromDayLog: Boolean
    ): FoodParseResult {
        val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val dataUrl = "data:$mime;base64,$b64"
        val response = api.visionCompletions(
            authorization = authHeader(),
            body = VisionRequest(
                model = vlmModel.ifBlank { BuildConfig.DEFAULT_VLM_MODEL },
                messages = listOf(
                    VisionMessage(
                        role = "system",
                        content = listOf(
                            VisionContentPart(
                                type = "text",
                                text = Prompts.visionFoodSystem(profile, effectiveWeightKg, date, weightFromDayLog)
                            )
                        )
                    ),
                    VisionMessage(
                        role = "user",
                        content = listOf(
                            VisionContentPart(type = "text", text = "请识别这顿饭并估算营养素 JSON"),
                            VisionContentPart(type = "image_url", imageUrl = ImageUrlPayload(dataUrl))
                        )
                    )
                )
            )
        )
        val raw = response.choices.firstOrNull()?.message?.content ?: error("VLM 未返回内容")
        return AiParsers.parseFood(raw)
    }

    suspend fun estimateWorkoutBurn(
        model: String,
        profile: UserProfileEntity,
        description: String,
        effectiveWeightKg: Double,
        date: String,
        weightFromDayLog: Boolean
    ): WorkoutBurnParse {
        val raw = chat(
            model,
            Prompts.workoutBurnSystem(profile, effectiveWeightKg, date, weightFromDayLog),
            "请估算以下运动消耗：\n$description",
            jsonMode = true
        )
        return AiParsers.parseWorkoutBurn(raw)
    }

    suspend fun estimateStrengthBurn(
        model: String,
        profile: UserProfileEntity,
        description: String,
        effectiveWeightKg: Double,
        date: String,
        weightFromDayLog: Boolean
    ): Int {
        val raw = chat(
            model,
            Prompts.strengthBurnSystem(profile, effectiveWeightKg, date, weightFromDayLog),
            description,
            jsonMode = true
        )
        return AiParsers.parseStrengthKcal(raw)
    }
}

