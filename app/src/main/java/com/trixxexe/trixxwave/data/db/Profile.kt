package com.trixxexe.trixxwave.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val avatarUri: String? = null
)
