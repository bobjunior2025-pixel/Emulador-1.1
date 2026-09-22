package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.db.SaveStateDao
import com.example.data.model.ConsoleType
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val gameDao: GameDao,
    private val saveStateDao: SaveStateDao
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val favoriteGames: Flow<List<GameEntity>> = gameDao.getFavoriteGames()

    fun getGamesByConsole(consoleType: ConsoleType): Flow<List<GameEntity>> {
        return gameDao.getGamesByConsole(consoleType)
    }

    suspend fun getGameById(id: Long): GameEntity? {
        return gameDao.getGameById(id)
    }

    suspend fun insertGame(game: GameEntity): Long {
        return gameDao.insertGame(game)
    }

    suspend fun deleteGame(game: GameEntity) {
        saveStateDao.deleteAllForGame(game.id)
        gameDao.deleteGame(game)
    }

    suspend fun toggleFavorite(gameId: Long, current: Boolean) {
        gameDao.updateFavorite(gameId, !current)
    }

    suspend fun recordPlaySession(gameId: Long, durationSeconds: Long) {
        gameDao.updatePlayTime(gameId, System.currentTimeMillis(), durationSeconds)
    }

    // Save States
    fun getSaveStates(gameId: Long): Flow<List<SaveStateEntity>> {
        return saveStateDao.getStatesForGame(gameId)
    }

    suspend fun saveState(state: SaveStateEntity): Long {
        val existing = saveStateDao.getStateBySlot(state.gameId, state.slot)
        val entityToSave = if (existing != null) state.copy(id = existing.id) else state
        return saveStateDao.insertOrUpdateState(entityToSave)
    }

    suspend fun loadState(gameId: Long, slot: Int): SaveStateEntity? {
        return saveStateDao.getStateBySlot(gameId, slot)
    }

    suspend fun deleteState(state: SaveStateEntity) {
        saveStateDao.deleteState(state)
    }

    suspend fun deleteStateBySlot(gameId: Long, slot: Int) {
        val existing = saveStateDao.getStateBySlot(gameId, slot)
        if (existing != null) {
            saveStateDao.deleteState(existing)
        }
    }

    suspend fun ensureInitialGames() {
        if (gameDao.getGameCount() == 0) {
            com.example.data.db.AppDatabase.populateInitialGames(gameDao)
        }
    }
}
