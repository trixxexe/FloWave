package com.trixxexe.trixxwave.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val index: Int,
    val message: ChatMessage
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val id: String?,
    val choices: List<ChatChoice>?
)

interface OpenAiService {
    @POST
    suspend fun createChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    companion object {
        fun create(): OpenAiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/v1/") // Dummy base URL, @Url overrides
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenAiService::class.java)
        }
    }
}
