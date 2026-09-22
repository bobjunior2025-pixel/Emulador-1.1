package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConsoleType
import com.example.data.model.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayedTimestamp DESC, title ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE consoleType = :consoleType ORDER BY lastPlayedTimestamp DESC, title ASC")
    fun getGamesByConsole(consoleType: ConsoleType): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("UPDATE games SET lastPlayedTimestamp = :timestamp, playTimeSeconds = playTimeSeconds + :addedSeconds WHERE id = :id")
    suspend fun updatePlayTime(id: Long, timestamp: Long, addedSeconds: Long)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int
}
