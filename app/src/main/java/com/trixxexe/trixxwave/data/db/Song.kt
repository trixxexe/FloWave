package com.trixxexe.trixxwave.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtUri: String? = null,
    val streamUrl: String? = null,
    val filePath: String? = null,
    val durationMs: Long = 0,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val isLiked: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = 0,
    val originalUrl: String? = null,
    val source: String? = null
)
