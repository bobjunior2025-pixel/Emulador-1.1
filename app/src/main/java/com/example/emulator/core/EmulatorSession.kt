package com.example.emulator.core

import android.graphics.Canvas
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadState
import com.example.data.model.AspectRatioMode
import com.example.data.model.ConsoleType
import com.example.data.model.EmulatorSettings
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EmulatorSession(private val audio: RetroAudioSynthesizer) {

    val ps1Engine = PS1Engine(audio)
    val n64Engine = N64Engine(audio)

    private val _currentFps = MutableStateFlow(60)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1) // 1x, 2x, 4x
    val speedMultiplier: StateFlow<Int> = _speedMultiplier.asStateFlow()

    private val _currentGame = MutableStateFlow<GameEntity?>(null)
    val currentGame: StateFlow<GameEntity?> = _currentGame.asStateFlow()

    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    fun loadGame(game: GameEntity, settings: EmulatorSettings) {
        _currentGame.value = game
        _isPaused.value = false
        _speedMultiplier.value = 1

        if (game.consoleType == ConsoleType.PS1) {
            ps1Engine.reset()
            if (settings.playPs1StartupChime) {
                audio.playPs1StartupChord()
            }
        } else {
            n64Engine.reset()
            audio.playN64Coin()
        }
    }

    fun togglePause() {
        _isPaused.update { !it }
    }

    fun cycleSpeed() {
        _speedMultiplier.update { current ->
            when (current) {
                1 -> 2
                2 -> 4
                else -> 1
            }
        }
    }

    fun restartGame(settings: EmulatorSettings) {
        val game = _currentGame.value ?: return
        loadGame(game, settings)
    }

    fun updateFrame(gamepad: GamepadState, dt: Float) {
        if (_isPaused.value) return

        val speed = _speedMultiplier.value
        val effectiveDt = dt * speed

        val game = _currentGame.value ?: return
        if (game.consoleType == ConsoleType.PS1) {
            ps1Engine.update(gamepad, effectiveDt)
        } else {
            n64Engine.update(gamepad, effectiveDt)
        }

        // FPS calculation
        frameCounter++
        val now = System.currentTimeMillis()
        if (now - lastFpsTimestamp >= 1000) {
            _currentFps.value = (frameCounter * 1000 / (now - lastFpsTimestamp)).toInt()
            frameCounter = 0
            lastFpsTimestamp = now
        }
    }

    fun renderFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        settings: EmulatorSettings
    ) {
        val game = _currentGame.value ?: return
        if (game.consoleType == ConsoleType.PS1) {
            ps1Engine.render(
                canvas,
                width,
                height,
                settings.scanlinesEnabled,
                settings.ps1Dithering
            )
        } else {
            n64Engine.render(
                canvas,
                width,
                height,
                settings.scanlinesEnabled,
                settings.n64ExpansionPak
            )
        }
    }

    fun captureSaveState(slot: Int): SaveStateEntity? {
        val game = _currentGame.value ?: return null
        return if (game.consoleType == ConsoleType.PS1) {
            val speedKmh = ps1Engine.speed.toInt()
            SaveStateEntity(
                gameId = game.id,
                slot = slot,
                timestamp = System.currentTimeMillis(),
                summary = "Volta ${ps1Engine.lap}/3 • ${speedKmh} km/h • Marcha ${ps1Engine.gear}",
                score = (ps1Engine.lapProgress * 10).toInt(),
                levelOrStage = "Circuito Especial (Volta ${ps1Engine.lap})",
                simulationDataJson = "${ps1Engine.playerX},${ps1Engine.speed},${ps1Engine.trackZ},${ps1Engine.gear},${ps1Engine.lap},${ps1Engine.lapProgress},${ps1Engine.steerAngle},${ps1Engine.cameraMode}"
            )
        } else {
            SaveStateEntity(
                gameId = game.id,
                slot = slot,
                timestamp = System.currentTimeMillis(),
                summary = "★ ${n64Engine.starCount} Estrelas • ● ${n64Engine.coinCount} Moedas • Vida ${n64Engine.health}/8",
                score = n64Engine.coinCount * 100,
                levelOrStage = "Ilha Flutuante dos Polígonos",
                simulationDataJson = "${n64Engine.posX},${n64Engine.posY},${n64Engine.posZ},${n64Engine.starCount},${n64Engine.coinCount},${n64Engine.velX},${n64Engine.velY},${n64Engine.velZ},${n64Engine.playerAngle},${n64Engine.cameraAngle},${n64Engine.cameraPitch},${n64Engine.cameraDistance},${n64Engine.health}"
            )
        }
    }

    fun restoreSaveState(state: SaveStateEntity) {
        val game = _currentGame.value ?: return
        try {
            val parts = state.simulationDataJson.split(",")
            if (game.consoleType == ConsoleType.PS1 && parts.size >= 6) {
                ps1Engine.playerX = parts[0].toFloatOrNull() ?: 0f
                ps1Engine.speed = parts[1].toFloatOrNull() ?: 0f
                ps1Engine.trackZ = parts[2].toFloatOrNull() ?: 0f
                ps1Engine.gear = parts[3].toIntOrNull() ?: 1
                ps1Engine.lap = parts[4].toIntOrNull() ?: 1
                ps1Engine.lapProgress = parts[5].toFloatOrNull() ?: 0f
                if (parts.size >= 8) {
                    ps1Engine.steerAngle = parts[6].toFloatOrNull() ?: 0f
                    ps1Engine.cameraMode = parts[7].toIntOrNull() ?: 0
                }
                audio.playEnginePulse(0.6f)
            } else if (game.consoleType == ConsoleType.N64 && parts.size >= 5) {
                n64Engine.posX = parts[0].toFloatOrNull() ?: 0f
                n64Engine.posY = parts[1].toFloatOrNull() ?: 0f
                n64Engine.posZ = parts[2].toFloatOrNull() ?: 0f
                n64Engine.starCount = parts[3].toIntOrNull() ?: 0
                n64Engine.coinCount = parts[4].toIntOrNull() ?: 0
                if (parts.size >= 13) {
                    n64Engine.velX = parts[5].toFloatOrNull() ?: 0f
                    n64Engine.velY = parts[6].toFloatOrNull() ?: 0f
                    n64Engine.velZ = parts[7].toFloatOrNull() ?: 0f
                    n64Engine.playerAngle = parts[8].toFloatOrNull() ?: 0f
                    n64Engine.cameraAngle = parts[9].toFloatOrNull() ?: 0f
                    n64Engine.cameraPitch = parts[10].toFloatOrNull() ?: 0.35f
                    n64Engine.cameraDistance = parts[11].toFloatOrNull() ?: 280f
                    n64Engine.health = parts[12].toIntOrNull() ?: 8
                }
                audio.playN64Coin()
            }
        } catch (_: Exception) {
        }
    }
}
