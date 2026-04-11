package com.nammaspendai.data.remote

import com.google.gson.annotations.SerializedName
import com.nammaspendai.data.model.ParsedExpense
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApiService {

    @POST("chat/completions")
    suspend fun parseExpenseText(
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}

data class GroqChatRequest(
    @SerializedName("model")
    val model: String = "llama3-70b-8192",

    @SerializedName("messages")
    val messages: List<Message>,

    @SerializedName("temperature")
    val temperature: Double = 0.1,

    @SerializedName("max_tokens")
    val maxTokens: Int = 1000,

    @SerializedName("response_format")
    val responseFormat: ResponseFormat? = ResponseFormat()
)

data class Message(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)

data class ResponseFormat(
    @SerializedName("type")
    val type: String = "json_object"
)

data class GroqChatResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("choices")
    val choices: List<Choice>,

    @SerializedName("usage")
    val usage: TokenUsage
)

data class Choice(
    @SerializedName("message")
    val message: MessageResponse
)

data class MessageResponse(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String // This will be the JSON string
)

data class TokenUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,

    @SerializedName("completion_tokens")
    val completionTokens: Int,

    @SerializedName("total_tokens")
    val totalTokens: Int
)
