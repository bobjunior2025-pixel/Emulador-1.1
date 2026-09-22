package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveStateDao {
    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY slot ASC")
    fun getStatesForGame(gameId: Long): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId AND slot = :slot LIMIT 1")
    suspend fun getStateBySlot(gameId: Long, slot: Int): SaveStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(state: SaveStateEntity): Long

    @Delete
    suspend fun deleteState(state: SaveStateEntity)

    @Query("DELETE FROM save_states WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: Long)
}
