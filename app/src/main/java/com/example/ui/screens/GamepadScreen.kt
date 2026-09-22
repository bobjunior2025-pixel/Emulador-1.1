package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controller.GamepadState
import com.example.ui.MainViewModel
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

@Composable
fun GamepadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val gamepadState by viewModel.gamepadState.collectAsStateWithLifecycle()
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsStateWithLifecycle()
    val settings by viewModel.emulatorSettings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Controles Bluetooth",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Compatível com controles de PlayStation, Xbox, Nintendo Switch, 8BitDo e genéricos.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Controller Connection Status Card
        item {
            ControllerStatusCard(
                gamepadState = gamepadState,
                onRefresh = {
                    viewModel.gamepadManager.refreshConnectedControllers()
                    viewModel.bluetoothGamepadService.refreshConnectedDevices()
                }
            )
        }

        // Bluetooth Devices List & Detection Status
        if (bluetoothDevices.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Text(
                                "Dispositivos Bluetooth Detectados",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        bluetoothDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceElevated)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${device.deviceCategory} • ${device.address}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (device.isConnected) Color(0xFF1B5E20) else SurfaceDark)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (device.isConnected) "ATIVO" else "PAREADO",
                                        color = if (device.isConnected) Color(0xFF81C784) else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Calibration & Diagnostic Visualizer Bench
        item {
            CalibrationVisualizerCard(gamepadState = gamepadState)
        }

        // Gamepad Settings
        item {
            Text(
                text = "Ajustes de Entrada e Sensibilidade",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Analog Deadzone Slider
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zona Morta do Analógico (Deadzone)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${(settings.analogDeadzone * 100).toInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("Evita desvio indesejado de alavancas analógicas gastas.", color = TextMuted, fontSize = 12.sp)

                    Slider(
                        value = settings.analogDeadzone,
                        onValueChange = { viewModel.updateSettings { curr -> curr.copy(analogDeadzone = it) } },
                        valueRange = 0.05f..0.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("deadzone_slider")
                    )
                }
            }
        }

        // Virtual Touch Overlay Opacity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Opacidade dos Controles Virtuais", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${(settings.touchControlsOpacity * 100).toInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Slider(
                        value = settings.touchControlsOpacity,
                        onValueChange = { viewModel.updateSettings { curr -> curr.copy(touchControlsOpacity = it) } },
                        valueRange = 0.2f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceElevated
                        )
                    )
                }
            }
        }

        // Toggles: Auto-hide and Haptics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Auto Hide
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ocultar Touch com Bluetooth", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Oculta botões da tela automaticamente ao conectar controle físico.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.autoHideTouchWhenControllerConnected,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(autoHideTouchWhenControllerConnected = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    // Haptic Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibração e Resposta Háptica", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Feedback de rumble ao pular, colidir e acelerar.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(hapticFeedbackEnabled = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Pairing Guide Card
        item {
            BluetoothPairingGuideCard()
        }
    }
}

@Composable
private fun ControllerStatusCard(
    gamepadState: GamepadState,
    onRefresh: () -> Unit
) {
    val isConnected = gamepadState.isControllerConnected

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isConnected) Color(0xFF0F261B) else SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) Color(0xFF00E676) else SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF00E676) else SurfaceElevated)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isConnected) BackgroundDark else TextSecondary
                    )
                }

                Column {
                    Text(
                        text = if (isConnected) "Controle Conectado" else "Nenhum Controle Físico",
                        color = if (isConnected) Color(0xFF00E676) else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = gamepadState.controllerName,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Tipo: ${gamepadState.controllerType}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar Controles", tint = TextPrimary)
            }
        }
    }
}

@Composable
private fun CalibrationVisualizerCard(gamepadState: GamepadState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bancada de Teste em Tempo Real",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Movimente alavancas e botões",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Dual Analog Stick Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left Stick
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Analógico Esquerdo", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AnalogStickGauge(x = gamepadState.leftStickX, y = gamepadState.leftStickY)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "X: %.2f  Y: %.2f".format(gamepadState.leftStickX, gamepadState.leftStickY),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Right Stick
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Analógico Direito / C-Btns", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AnalogStickGauge(x = gamepadState.rightStickX, y = gamepadState.rightStickY)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "X: %.2f  Y: %.2f".format(gamepadState.rightStickX, gamepadState.rightStickY),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Analog Triggers Pressure (L2 and R2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gatilho L2: ${(gamepadState.leftTrigger * 100).toInt()}%", color = TextSecondary, fontSize = 11.sp)
                    Text("Gatilho R2: ${(gamepadState.rightTrigger * 100).toInt()}%", color = TextSecondary, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { gamepadState.leftTrigger },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = SurfaceElevated
                    )
                    LinearProgressIndicator(
                        progress = { gamepadState.rightTrigger },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = SurfaceElevated
                    )
                }
            }

            // Buttons Matrix Indicators (Lights up when physically pressed)
            Text("Botões Pressionados:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ButtonPressPill(label = "A / ✕", isPressed = gamepadState.ps1Cross || gamepadState.n64A)
                ButtonPressPill(label = "B / ○", isPressed = gamepadState.ps1Circle || gamepadState.n64B)
                ButtonPressPill(label = "X / □", isPressed = gamepadState.ps1Square)
                ButtonPressPill(label = "Y / △", isPressed = gamepadState.ps1Triangle)
                ButtonPressPill(label = "Z / L2", isPressed = gamepadState.n64Z || gamepadState.l2)
                ButtonPressPill(label = "L1", isPressed = gamepadState.l1)
                ButtonPressPill(label = "R1", isPressed = gamepadState.r1)
                ButtonPressPill(label = "START", isPressed = gamepadState.start)
            }

            // Diagnostic event log
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0E14))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Último evento: ${gamepadState.lastEventSummary}",
                    color = Color(0xFF82B1FF),
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AnalogStickGauge(x: Float, y: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(Color(0xFF161922))
            .border(1.5.dp, Color(0xFF2C3244), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.width / 2f - 10f

            // Crosshair
            drawLine(Color(0xFF2C3244), Offset(0f, center.y), Offset(size.width, center.y), 1.5f)
            drawLine(Color(0xFF2C3244), Offset(center.x, 0f), Offset(center.x, size.height), 1.5f)

            // Deadzone circle (15%)
            drawCircle(Color(0x3300E5FF), radius = maxR * 0.15f, center = center)

            // Current Stick Position Indicator Dot
            val dotX = center.x + (x * maxR)
            val dotY = center.y + (y * maxR)
            drawCircle(Color(0xFF00E5FF), radius = 9f, center = Offset(dotX, dotY))
        }
    }
}

@Composable
private fun ButtonPressPill(label: String, isPressed: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0xFF00E676) else SurfaceElevated)
            .border(1.dp, if (isPressed) Color(0xFF00E676) else SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isPressed) BackgroundDark else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BluetoothPairingGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Como parear seu controle Bluetooth no Android:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("• PlayStation (PS4 / PS5): Pressione SHARE + Botão PS até a barra de luz piscar em azul.", color = TextSecondary, fontSize = 12.sp)
            Text("• Xbox Wireless: Ligue o controle e segure o botão de pareamento no topo.", color = TextSecondary, fontSize = 12.sp)
            Text("• Nintendo Switch / 8BitDo: Pressione o botão SYNC no topo do controle.", color = TextSecondary, fontSize = 12.sp)
            Text("• Vá nas Configurações de Bluetooth do Android e selecione o controle para conectar.", color = NeonCyan, fontSize = 12.sp)
        }
    }
}
