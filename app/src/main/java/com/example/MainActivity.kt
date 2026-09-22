package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.EmulatorScreen
import com.example.ui.screens.GamepadScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RetroPlayTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RetroPlayTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    /**
     * Intercept physical hardware buttons from Bluetooth / USB gamepads
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.gamepadManager.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Intercept analog thumbsticks and analog triggers (L2/R2) from Bluetooth gamepads
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (viewModel.gamepadManager.handleMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentGame by viewModel.currentGame.collectAsStateWithLifecycle()
    val gamepadState by viewModel.gamepadState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display messages via Snackbar
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Android Hardware Back button handling
    BackHandler(enabled = activeTab == AppTab.EMULATOR) {
        viewModel.closeGame()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Hide navigation bar when in Emulator mode for full immersion
            if (activeTab != AppTab.EMULATOR) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    // Library Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.LIBRARY,
                        onClick = { viewModel.selectTab(AppTab.LIBRARY) },
                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Biblioteca") },
                        label = { Text("Jogos", fontSize = 11.sp) },
                        colors = navigationBarColors()
                    )

                    // Emulator Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.EMULATOR,
                        onClick = {
                            if (currentGame != null) {
                                viewModel.selectTab(AppTab.EMULATOR)
                            } else {
                                viewModel.showToast("Selecione um jogo na Biblioteca primeiro")
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (currentGame != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan)
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = "Emulador")
                            }
                        },
                        label = { Text("Jogar", fontSize = 11.sp) },
                        colors = navigationBarColors()
                    )

                    // Bluetooth Gamepad Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.CONTROLLER,
                        onClick = { viewModel.selectTab(AppTab.CONTROLLER) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (gamepadState.isControllerConnected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E676))
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (gamepadState.isControllerConnected) Icons.Default.BluetoothConnected else Icons.Default.Gamepad,
                                    contentDescription = "Controles"
                                )
                            }
                        },
                        label = { Text("Controles", fontSize = 11.sp) },
                        colors = navigationBarColors()
                    )

                    // Settings Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.SETTINGS,
                        onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes", fontSize = 11.sp) },
                        colors = navigationBarColors()
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (activeTab == AppTab.EMULATOR) 0.dp else innerPadding.calculateBottomPadding(),
                    top = innerPadding.calculateTopPadding()
                )
        ) { tab ->
            when (tab) {
                AppTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                AppTab.EMULATOR -> EmulatorScreen(viewModel = viewModel)
                AppTab.CONTROLLER -> GamepadScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = BackgroundDark,
    selectedTextColor = NeonCyan,
    indicatorColor = NeonCyan,
    unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted
)
