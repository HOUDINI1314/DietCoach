package com.dietcoach.app.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    val stream: Boolean? = null,
    @Json(name = "response_format") val responseFormat: ResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class ChatStreamChunk(
    val choices: List<StreamChoice> = emptyList()
) {
    @JsonClass(generateAdapter = true)
    data class StreamChoice(
        val delta: StreamDelta? = null,
        @Json(name = "finish_reason") val finishReason: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class StreamDelta(
        val role: String? = null,
        val content: String? = null,
        @Json(name = "reasoning_content") val reasoningContent: String? = null
    )
}

@JsonClass(generateAdapter = true)
data class ResponseFormat(val type: String)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val choices: List<Choice> = emptyList()
) {
    @JsonClass(generateAdapter = true)
    data class Choice(
        val message: ChatMessage? = null
    )
}

@JsonClass(generateAdapter = true)
data class ImageUrlPayload(val url: String)

@JsonClass(generateAdapter = true)
data class VisionContentPart(
    val type: String,
    val text: String? = null,
    @Json(name = "image_url") val imageUrl: ImageUrlPayload? = null
)

@JsonClass(generateAdapter = true)
data class VisionMessage(
    val role: String,
    val content: List<VisionContentPart>
)

@JsonClass(generateAdapter = true)
data class VisionRequest(
    val model: String,
    val messages: List<VisionMessage>,
    val temperature: Double = 0.2
)

@JsonClass(generateAdapter = true)
data class VisionResponse(
    val choices: List<VisionChoice> = emptyList()
) {
    @JsonClass(generateAdapter = true)
    data class VisionChoice(
        val message: VisionMessageOut? = null
    )
}

@JsonClass(generateAdapter = true)
data class VisionMessageOut(
    val role: String? = null,
    val content: String? = null
)

interface DashScopeApi {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): ChatResponse

    @POST("chat/completions")
    suspend fun visionCompletions(
        @Header("Authorization") authorization: String,
        @Body body: VisionRequest
    ): VisionResponse
}
