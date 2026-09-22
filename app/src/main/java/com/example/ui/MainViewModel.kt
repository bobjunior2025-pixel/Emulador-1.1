package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadManager
import com.example.controller.GamepadState
import com.example.data.db.AppDatabase
import com.example.data.model.AspectRatioMode
import com.example.data.model.ConsoleType
import com.example.data.model.EmulatorSettings
import com.example.data.model.GameEntity
import com.example.data.model.ResolutionScale
import com.example.data.model.SaveStateEntity
import com.example.data.repository.GameRepository
import com.example.emulator.core.EmulatorSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val iconLabel: String) {
    LIBRARY("Biblioteca", "Jogos"),
    EMULATOR("Emulador", "Jogar"),
    CONTROLLER("Controles", "Bluetooth"),
    SETTINGS("Ajustes", "Config")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val audioSynthesizer = RetroAudioSynthesizer(application)
    val gamepadManager = GamepadManager(application)
    val emulatorSession = EmulatorSession(audioSynthesizer)

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = GameRepository(db.gameDao(), db.saveStateDao())

    private val _activeTab = MutableStateFlow(AppTab.LIBRARY)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _selectedConsoleFilter = MutableStateFlow<ConsoleType?>(null)
    val selectedConsoleFilter: StateFlow<ConsoleType?> = _selectedConsoleFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val _emulatorSettings = MutableStateFlow(EmulatorSettings())
    val emulatorSettings: StateFlow<EmulatorSettings> = _emulatorSettings.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val gamepadState: StateFlow<GamepadState> = gamepadManager.gamepadState
    val currentGame: StateFlow<GameEntity?> = emulatorSession.currentGame
    val currentFps: StateFlow<Int> = emulatorSession.currentFps
    val isPaused: StateFlow<Boolean> = emulatorSession.isPaused
    val speedMultiplier: StateFlow<Int> = emulatorSession.speedMultiplier

    // Reactive list of games with filtering
    val games: StateFlow<List<GameEntity>> = combine(
        repository.allGames,
        _selectedConsoleFilter,
        _searchQuery,
        _showOnlyFavorites
    ) { all, consoleFilter, query, favoritesOnly ->
        all.filter { game ->
            val matchesConsole = consoleFilter == null || game.consoleType == consoleFilter
            val matchesQuery = query.isBlank() || game.title.contains(query, ignoreCase = true)
            val matchesFav = !favoritesOnly || game.isFavorite
            matchesConsole && matchesQuery && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active game save states
    val activeSaveStates: StateFlow<List<SaveStateEntity>> = currentGame
        .flatMapLatest { game ->
            if (game != null) repository.getSaveStates(game.id)
            else MutableStateFlow(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var gameLoopJob: Job? = null
    private var sessionStartTimestamp = 0L

    init {
        viewModelScope.launch {
            repository.ensureInitialGames()
        }
    }

    override fun onCleared() {
        super.onCleared()
        gamepadManager.release()
        stopGameLoop()
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setConsoleFilter(console: ConsoleType?) {
        _selectedConsoleFilter.value = console
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.update { !it }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showToast(msg: String) {
        _userMessage.value = msg
    }

    fun playGame(game: GameEntity) {
        // Record play duration of previous game if any
        endCurrentSession()

        sessionStartTimestamp = System.currentTimeMillis()
        emulatorSession.loadGame(game, _emulatorSettings.value)
        _activeTab.value = AppTab.EMULATOR
        startGameLoop()
        showToast("Carregando ${game.title}...")
    }

    fun closeGame() {
        endCurrentSession()
        stopGameLoop()
        _activeTab.value = AppTab.LIBRARY
    }

    private fun endCurrentSession() {
        val game = currentGame.value ?: return
        if (sessionStartTimestamp > 0) {
            val durationSec = (System.currentTimeMillis() - sessionStartTimestamp) / 1000
            if (durationSec > 2) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.recordPlaySession(game.id, durationSec)
                }
            }
        }
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                val pad = gamepadManager.gamepadState.value
                emulatorSession.updateFrame(pad, dt)

                // ~60 FPS update interval
                delay(16)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(game.id, game.isFavorite)
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGame(game)
            showToast("Jogo removido da biblioteca")
        }
    }

    fun importRomFromUri(uri: Uri, displayName: String, sizeBytes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val extension = displayName.substringAfterLast('.', "").lowercase()

            val consoleType = when {
                extension in listOf("iso", "bin", "cue", "pbp", "img") -> ConsoleType.PS1
                extension in listOf("z64", "n64", "v64", "rom") -> ConsoleType.N64
                displayName.contains("ps1", ignoreCase = true) || displayName.contains("psx", ignoreCase = true) -> ConsoleType.PS1
                else -> ConsoleType.N64
            }

            val cleanTitle = displayName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            val coverDrawable = if (consoleType == ConsoleType.PS1) "ps1_game_hero_1790054759614" else "n64_game_hero_1790054771488"

            val newGame = GameEntity(
                title = cleanTitle,
                consoleType = consoleType,
                filePath = uri.toString(),
                coverDrawableName = coverDrawable,
                fileSizeBytes = sizeBytes,
                lastPlayedTimestamp = System.currentTimeMillis(),
                isFavorite = false,
                isBuiltInDemo = false,
                description = "ROM importada do armazenamento local (${consoleType.displayName})."
            )

            val id = repository.insertGame(newGame)
            showToast("ROM importada: $cleanTitle ($consoleType)")

            // Auto switch to game
            playGame(newGame.copy(id = id))
        }
    }

    fun saveState(slot: Int, summaryOverride: String? = null) {
        val state = emulatorSession.captureSaveState(slot)
        if (state != null) {
            val finalState = if (summaryOverride != null) state.copy(summary = summaryOverride) else state
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveState(finalState)
                audioSynthesizer.triggerVibration(45, 190)
                val label = if (slot == 0) "Auto-Save" else "Slot $slot"
                showToast("Save State gravado com sucesso no $label!")
            }
        }
    }

    fun loadState(slot: Int) {
        val game = currentGame.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.loadState(game.id, slot)
            if (state != null) {
                emulatorSession.restoreSaveState(state)
                audioSynthesizer.triggerVibration(60, 220)
                val label = if (slot == 0) "Auto-Save" else "Slot $slot"
                showToast("Save State restaurado do $label!")
            } else {
                val label = if (slot == 0) "Auto-Save" else "Slot $slot"
                showToast("Nenhum snapshot encontrado no $label")
            }
        }
    }

    fun deleteState(slot: Int) {
        val game = currentGame.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStateBySlot(game.id, slot)
            val label = if (slot == 0) "Auto-Save" else "Slot $slot"
            showToast("Snapshot do $label removido.")
        }
    }

    fun autoSaveCurrentGame() {
        saveState(slot = 0, summaryOverride = null)
    }

    fun updateSettings(transform: (EmulatorSettings) -> EmulatorSettings) {
        _emulatorSettings.update { current ->
            val updated = transform(current)
            audioSynthesizer.isEnabled = updated.audioEnabled
            audioSynthesizer.volume = updated.audioVolume
            gamepadManager.deadzone = updated.analogDeadzone
            updated
        }
    }
}
