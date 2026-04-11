package com.nammaspendai.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.nammaspendai.BuildConfig
import com.nammaspendai.data.model.ParsedExpense
import com.nammaspendai.data.model.ParsedExpensesResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqClient @Inject constructor() {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.GROQ_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: GroqApiService = retrofit.create(GroqApiService::class.java)

    suspend fun parseVoiceInput(text: String): Result<List<ParsedExpense>> {
        return try {
            val prompt = buildPrompt(text)
            val request = GroqChatRequest(
                messages = listOf(
                    Message(role = "system", content = SYSTEM_PROMPT),
                    Message(role = "user", content = prompt)
                )
            )

            val response = apiService.parseExpenseText(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.choices.isNotEmpty()) {
                    val content = body.choices.first().message.content
                    parseResponse(content)
                } else {
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(text: String): String {
        return """Convert the following expense/income description into structured JSON.

Input: "$text"

Respond ONLY with a JSON object in this format:
{
  "expenses": [
    {
      "amount": number,
      "type": "expense" or "income",
      "category": "category_name",
      "merchant": "merchant_name or null",
      "description": "brief description or null",
      "timestamp": "ISO-8601 datetime or null"
    }
  ]
}

Categories: Food, Shopping, Transport, Entertainment, Utilities, Recharge, Medical, Fuel, Finance, Education, Travel, Gifts, Income, Other

Rules:
1. Amount must be a positive number
2. Type is "expense" for spending, "income" for money received
3. Infer category from context
4. Extract merchant if mentioned
5. Return only valid JSON, no markdown, no explanation"""
    }

    private fun parseResponse(content: String): Result<List<ParsedExpense>> {
        return try {
            val gson = Gson()
            val response = gson.fromJson(content, ParsedExpensesResponse::class.java)
            Result.success(response.expenses)
        } catch (e: JsonParseException) {
            // Try to extract JSON from markdown code block if present
            val jsonRegex = """```json\s*([\s\S]*?)```""".toRegex()
            val match = jsonRegex.find(content)
            if (match != null) {
                try {
                    val json = match.groupValues[1]
                    val response = Gson().fromJson(json, ParsedExpensesResponse::class.java)
                    Result.success(response.expenses)
                } catch (e2: Exception) {
                    Result.failure(Exception("Failed to parse AI response: ${e.message}"))
                }
            } else {
                Result.failure(Exception("Failed to parse AI response: ${e.message}"))
            }
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """You are an expense tracking assistant. Convert natural language expense descriptions into structured JSON data. Be accurate with amounts and infer categories intelligently. Always respond with valid JSON only."""
    }
}
