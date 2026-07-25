package com.trixxexe.trixxwave.data.api

import com.trixxexe.trixxwave.data.preferences.AiConfig

class AiRepository(private val openAiService: OpenAiService) {

    private fun getEndpointUrl(config: AiConfig): String {
        var endpoint = config.customEndpoint.trim()
        if (!endpoint.endsWith("/")) {
            endpoint += "/"
        }
        return if (endpoint.contains("chat/completions")) {
            endpoint
        } else {
            "${endpoint}chat/completions"
        }
    }

    suspend fun testConnection(config: AiConfig): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API Key is missing"))
        }
        return try {
            val response = openAiService.createChatCompletion(
                url = getEndpointUrl(config),
                authorization = "Bearer ${config.apiKey}",
                request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(
                        ChatMessage("user", "Hello! Respond with 'FloWave Connected' if active.")
                    ),
                    temperature = 0.3
                )
            )
            if (response.isSuccessful) {
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "Success"
                Result.success(text)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTrackInsights(config: AiConfig, title: String, artist: String, album: String): String {
        if (config.apiKey.isBlank()) return "Configure AI Provider in Settings to view track insights."
        return try {
            val prompt = "Provide a brief, engaging 2-sentence background story and musical analysis for the song '$title' by $artist from album '$album'."
            val response = openAiService.createChatCompletion(
                url = getEndpointUrl(config),
                authorization = "Bearer ${config.apiKey}",
                request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.7
                )
            )
            response.body()?.choices?.firstOrNull()?.message?.content ?: "No insights generated."
        } catch (e: Exception) {
            "Could not fetch insights: ${e.localizedMessage}"
        }
    }

    suspend fun generateMoodTags(config: AiConfig, title: String, artist: String): String {
        if (config.apiKey.isBlank()) return "Energetic, Melodic, Chill"
        return try {
            val prompt = "Respond with exactly 3 comma-separated mood/genre adjectives describing '$title' by $artist. Example: Energetic, Synthwave, Cyberpunk. Nothing else."
            val response = openAiService.createChatCompletion(
                url = getEndpointUrl(config),
                authorization = "Bearer ${config.apiKey}",
                request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(ChatMessage("user", prompt)),
                    temperature = 0.5
                )
            )
            response.body()?.choices?.firstOrNull()?.message?.content?.trim() ?: "Chill, Melodic, Ambient"
        } catch (e: Exception) {
            "Chill, Ambient, Vocal"
        }
    }

    suspend fun selectSmartMixSongs(config: AiConfig, prompt: String, availableSongs: List<String>): List<String> {
        if (config.apiKey.isBlank() || availableSongs.isEmpty()) return availableSongs.take(15)
        return try {
            val songListStr = availableSongs.take(100).joinToString("\n")
            val systemPrompt = "You are a smart music curator. From the list of available songs provided, pick up to 15 titles that best fit the prompt: '$prompt'. Return ONLY a JSON array of exact song title strings e.g. [\"Song A\", \"Song B\"]."
            val userMsg = "Available Songs:\n$songListStr"

            val response = openAiService.createChatCompletion(
                url = getEndpointUrl(config),
                authorization = "Bearer ${config.apiKey}",
                request = ChatCompletionRequest(
                    model = config.modelName,
                    messages = listOf(
                        ChatMessage("system", systemPrompt),
                        ChatMessage("user", userMsg)
                    ),
                    temperature = 0.4
                )
            )
            val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
            // Simple parsing for titles in brackets
            val regex = """"([^"]+)"""".toRegex()
            val matchedTitles = regex.findAll(content).map { it.groupValues[1] }.toList()
            if (matchedTitles.isNotEmpty()) matchedTitles else availableSongs.take(15)
        } catch (e: Exception) {
            availableSongs.take(15)
        }
    }
}
