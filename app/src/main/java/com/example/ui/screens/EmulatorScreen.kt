package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AspectRatioMode
import com.example.data.model.ConsoleType
import com.example.ui.MainViewModel
import com.example.ui.controls.N64TouchControls
import com.example.ui.controls.PS1TouchControls
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.N64Gold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PS1Blue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.isActive

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentGame by viewModel.currentGame.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.speedMultiplier.collectAsStateWithLifecycle()
    val fps by viewModel.currentFps.collectAsStateWithLifecycle()
    val settings by viewModel.emulatorSettings.collectAsStateWithLifecycle()
    val gamepadState by viewModel.gamepadState.collectAsStateWithLifecycle()
    val activeSaveStates by viewModel.activeSaveStates.collectAsStateWithLifecycle()

    var manualShowTouchControls by remember { mutableStateOf(true) }
    var showSaveStateSheet by remember { mutableStateOf(false) }

    // Frame ticker for Canvas recomposition/redraw
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameNano ->
                frameTick = frameNano
            }
        }
    }

    if (currentGame == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Nenhum jogo em execução.\nSelecione um jogo na Biblioteca.",
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    val game = currentGame!!
    val isPS1 = game.consoleType == ConsoleType.PS1

    // Determine whether to display on-screen touch controls
    val shouldShowTouch = if (settings.autoHideTouchWhenControllerConnected && gamepadState.isControllerConnected) {
        !manualShowTouchControls
    } else {
        manualShowTouchControls
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Status Bar & Controls
            EmulatorTopBar(
                title = game.title,
                consoleType = game.consoleType,
                fps = fps,
                isPaused = isPaused,
                speed = speedMultiplier,
                isGamepadConnected = gamepadState.isControllerConnected,
                controllerName = gamepadState.controllerName,
                onBack = {
                    viewModel.autoSaveCurrentGame()
                    viewModel.closeGame()
                },
                onTogglePause = { viewModel.emulatorSession.togglePause() },
                onCycleSpeed = { viewModel.emulatorSession.cycleSpeed() },
                onQuickSave = { viewModel.saveState(1) },
                onQuickLoad = { viewModel.loadState(1) },
                onOpenSaveManager = { showSaveStateSheet = true }
            )

            // Game Display Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Apply Aspect Ratio Constraint
                val aspectModifier = when (settings.aspectRatio) {
                    AspectRatioMode.ORIGINAL_4_3 -> Modifier.aspectRatio(4f / 3f)
                    AspectRatioMode.WIDESCREEN_16_9 -> Modifier.aspectRatio(16f / 9f)
                    AspectRatioMode.STRETCH -> Modifier.fillMaxSize()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = aspectModifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            .testTag("emulator_canvas")
                    ) {
                        // Read tick to guarantee invalidation each frame
                        val tick = frameTick
                        drawIntoCanvas { composeCanvas ->
                            val nativeCanvas = composeCanvas.nativeCanvas
                            viewModel.emulatorSession.renderFrame(
                                nativeCanvas,
                                size.width,
                                size.height,
                                settings
                            )
                        }
                    }

                    // CRT Scanlines Overlay
                    if (settings.scanlinesEnabled) {
                        Canvas(
                            modifier = aspectModifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val scanlinePaint = Paint().apply {
                                color = android.graphics.Color.argb(
                                    (settings.scanlineIntensity * 120).toInt(),
                                    0,
                                    0,
                                    0
                                )
                                strokeWidth = 2f
                            }
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = Color(0x33000000),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 2f
                                )
                                y += 4f
                            }
                        }
                    }

                    // Paused Overlay Banner
                    if (isPaused) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "PAUSADO",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            // Quick Floating Toolbar (Aspect ratio, Scanlines, Restart, Hide Touch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Aspect ratio cycle
                    IconButton(
                        onClick = {
                            viewModel.updateSettings { curr ->
                                val next = when (curr.aspectRatio) {
                                    AspectRatioMode.ORIGINAL_4_3 -> AspectRatioMode.WIDESCREEN_16_9
                                    AspectRatioMode.WIDESCREEN_16_9 -> AspectRatioMode.STRETCH
                                    AspectRatioMode.STRETCH -> AspectRatioMode.ORIGINAL_4_3
                                }
                                curr.copy(aspectRatio = next)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspecto", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }

                    // Scanlines toggle
                    IconButton(
                        onClick = {
                            viewModel.updateSettings { it.copy(scanlinesEnabled = !it.scanlinesEnabled) }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Tv,
                            contentDescription = "CRT Scanlines",
                            tint = if (settings.scanlinesEnabled) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Restart
                    IconButton(
                        onClick = { viewModel.emulatorSession.restartGame(settings) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                // Save States quick button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .clickable { showSaveStateSheet = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("open_save_states_toolbar_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "Save States",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Saves",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bluetooth Gamepad indicator badge
                if (gamepadState.isControllerConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0D331E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Gamepad, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                        Text(gamepadState.controllerName, color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Toggle Touch Visibility
                IconButton(
                    onClick = { manualShowTouchControls = !manualShowTouchControls },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (shouldShowTouch) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Alternar Controles Touch",
                        tint = if (shouldShowTouch) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Lower Screen Virtual Touch Controls (Takes bottom half on portrait or overlay)
            if (shouldShowTouch) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(SurfaceDark)
                ) {
                    if (isPS1) {
                        PS1TouchControls(
                            gamepadManager = viewModel.gamepadManager,
                            audio = viewModel.audioSynthesizer,
                            opacity = settings.touchControlsOpacity
                        )
                    } else {
                        N64TouchControls(
                            gamepadManager = viewModel.gamepadManager,
                            audio = viewModel.audioSynthesizer,
                            opacity = settings.touchControlsOpacity
                        )
                    }
                }
            }
        }

        // Save & Load States Bottom Sheet
        if (showSaveStateSheet) {
            SaveStateBottomSheet(
                game = game,
                saveStates = activeSaveStates,
                onSaveToSlot = { slot ->
                    viewModel.saveState(slot)
                },
                onLoadFromSlot = { slot ->
                    viewModel.loadState(slot)
                    showSaveStateSheet = false
                },
                onDeleteSlot = { slot ->
                    viewModel.deleteState(slot)
                },
                onDismiss = {
                    showSaveStateSheet = false
                }
            )
        }
    }
}

@Composable
private fun EmulatorTopBar(
    title: String,
    consoleType: ConsoleType,
    fps: Int,
    isPaused: Boolean,
    speed: Int,
    isGamepadConnected: Boolean,
    controllerName: String,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    onOpenSaveManager: () -> Unit
) {
    val isPS1 = consoleType == ConsoleType.PS1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPS1) PS1Blue else N64Gold)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = consoleType.badge,
                    color = if (isPS1) Color.White else BackgroundDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // FPS Badge & Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // FPS Counter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("$fps FPS", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Speed multiplier pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (speed > 1) NeonCyan else SurfaceElevated)
                    .clickable { onCycleSpeed() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "${speed}X",
                    color = if (speed > 1) BackgroundDark else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Save / Load
            IconButton(
                onClick = onQuickSave,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("quick_save_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Salvar Slot 1", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onQuickLoad,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("quick_load_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Carregar Slot 1", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            // Save States Manager dialog button
            IconButton(
                onClick = onOpenSaveManager,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("manage_saves_top_button")
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = "Gerenciar Save States", tint = NeonCyan, modifier = Modifier.size(18.dp))
            }

            // Pause / Resume
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("pause_toggle_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Continuar" else "Pausar",
                    tint = if (isPaused) Color(0xFFFFAB00) else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
