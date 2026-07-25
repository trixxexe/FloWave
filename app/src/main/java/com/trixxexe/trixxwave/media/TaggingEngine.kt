package com.trixxexe.trixxwave.media

import com.trixxexe.trixxwave.data.api.AiRepository
import com.trixxexe.trixxwave.data.preferences.AiConfig
import kotlin.math.abs

object TaggingEngine {

    /**
     * Generates tags using AI if API key is present; otherwise falls back to deterministic local rule-based tagging.
     */
    suspend fun getMoodAndGenreTags(
        aiRepository: AiRepository,
        aiConfig: AiConfig,
        title: String,
        artist: String,
        album: String
    ): String {
        return if (aiConfig.apiKey.isNotBlank()) {
            try {
                val aiTags = aiRepository.generateMoodTags(aiConfig, title, artist)
                if (aiTags.isNotBlank() && !aiTags.startsWith("Could not")) {
                    aiTags
                } else {
                    generateLocalFallbackTags(title, artist, album)
                }
            } catch (e: Exception) {
                generateLocalFallbackTags(title, artist, album)
            }
        } else {
            generateLocalFallbackTags(title, artist, album)
        }
    }

    /**
     * Rule-based local heuristic tagger that works 100% offline without any API keys.
     */
    fun generateLocalFallbackTags(title: String, artist: String, album: String): String {
        val combined = "$title $artist $album".lowercase()

        val tags = mutableSetOf<String>()

        when {
            combined.containsAny("synth", "wave", "cyber", "neon", "retro", "80s", "electronic") -> {
                tags.addAll(listOf("Synthwave", "Cyberpunk", "Electronic"))
            }
            combined.containsAny("chill", "sleep", "lofi", "relax", "night", "rain", "ambient", "soft") -> {
                tags.addAll(listOf("Chill", "Lofi", "Ambient"))
            }
            combined.containsAny("rock", "metal", "heavy", "punk", "guitar", "drive", "storm") -> {
                tags.addAll(listOf("Energetic", "Rock", "High Energy"))
            }
            combined.containsAny("pop", "dance", "club", "party", "beat", "rhythm") -> {
                tags.addAll(listOf("Upbeat", "Pop", "Dance"))
            }
            combined.containsAny("piano", "acoustic", "classical", "violin", "orchestra") -> {
                tags.addAll(listOf("Acoustic", "Melodic", "Focus"))
            }
            else -> {
                // Deterministic fallback based on track title string hash
                val hash = abs("$title $artist".hashCode())
                val sets = listOf(
                    listOf("Melodic", "Chill", "Vocal"),
                    listOf("Energetic", "Rhythmic", "Electronic"),
                    listOf("Ambient", "Atmospheric", "Focus"),
                    listOf("Groove", "Upbeat", "Synthetic")
                )
                tags.addAll(sets[hash % sets.size])
            }
        }

        return tags.take(3).joinToString(", ")
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
