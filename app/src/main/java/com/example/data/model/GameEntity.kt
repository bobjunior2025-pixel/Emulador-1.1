package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val consoleType: ConsoleType,
    val filePath: String,
    val coverDrawableName: String = "",
    val fileSizeBytes: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val playTimeSeconds: Long = 0,
    val isFavorite: Boolean = false,
    val region: String = "USA",
    val isBuiltInDemo: Boolean = false,
    val description: String = ""
)
