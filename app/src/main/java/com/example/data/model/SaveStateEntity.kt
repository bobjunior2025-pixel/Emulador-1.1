package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_states")
data class SaveStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val slot: Int, // 1, 2, 3, or 0 for Auto
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String = "",
    val score: Int = 0,
    val levelOrStage: String = "Fase 1",
    val simulationDataJson: String = ""
)
