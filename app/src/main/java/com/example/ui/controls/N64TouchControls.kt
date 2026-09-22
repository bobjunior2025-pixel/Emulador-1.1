package com.example.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadManager
import com.example.controller.VirtualButton
import com.example.ui.theme.N64Blue
import com.example.ui.theme.N64Green
import com.example.ui.theme.N64Red
import com.example.ui.theme.N64Yellow

@Composable
fun N64TouchControls(
    gamepadManager: GamepadManager,
    audio: RetroAudioSynthesizer,
    opacity: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Shoulder Bumpers (L, Z Trigger, R)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            N64BumperButton(
                label = "L",
                onDown = { gamepadManager.setVirtualButton(VirtualButton.L1, true); audio.triggerVibration(25, 120) },
                onUp = { gamepadManager.setVirtualButton(VirtualButton.L1, false) }
            )

            // Prominent Z Trigger
            N64ZTriggerButton(
                onDown = { gamepadManager.setVirtualButton(VirtualButton.N64_Z, true); audio.triggerVibration(35, 160) },
                onUp = { gamepadManager.setVirtualButton(VirtualButton.N64_Z, false) }
            )

            N64BumperButton(
                label = "R",
                onDown = { gamepadManager.setVirtualButton(VirtualButton.R1, true); audio.triggerVibration(25, 120) },
                onUp = { gamepadManager.setVirtualButton(VirtualButton.R1, false) }
            )
        }

        // Center Red Start Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 68.dp, height = 30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(N64Red)
                    .border(1.5.dp, Color(0xFFFF7961), RoundedCornerShape(15.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                gamepadManager.setVirtualButton(VirtualButton.START, true)
                                audio.triggerVibration(30, 140)
                                tryAwaitRelease()
                                gamepadManager.setVirtualButton(VirtualButton.START, false)
                            }
                        )
                    }
                    .testTag("btn_n64_start")
            ) {
                Text("START", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        // Left: 3D Analog Joystick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 20.dp)
                .size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            VirtualAnalogStick(
                onStickMoved = { x, y -> gamepadManager.setVirtualStick(x, y) },
                onStickReleased = { gamepadManager.setVirtualStick(0f, 0f) }
            )
        }

        // Right: N64 Button Cluster (A, B, and C-buttons)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(170.dp)
        ) {
            // C-Buttons Cluster (Top Right of the cluster)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(105.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular base for C-buttons
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E212A))
                )

                // C-Up
                N64CButton(
                    symbol = "▲",
                    modifier = Modifier.align(Alignment.TopCenter),
                    onDown = { gamepadManager.setVirtualButton(VirtualButton.N64_C_UP, true); audio.triggerVibration(20, 100) },
                    onUp = { gamepadManager.setVirtualButton(VirtualButton.N64_C_UP, false) }
                )
                // C-Down
                N64CButton(
                    symbol = "▼",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onDown = { gamepadManager.setVirtualButton(VirtualButton.N64_C_DOWN, true); audio.triggerVibration(20, 100) },
                    onUp = { gamepadManager.setVirtualButton(VirtualButton.N64_C_DOWN, false) }
                )
                // C-Left
                N64CButton(
                    symbol = "◀",
                    modifier = Modifier.align(Alignment.CenterStart),
                    onDown = { gamepadManager.setVirtualButton(VirtualButton.N64_C_LEFT, true); audio.triggerVibration(20, 100) },
                    onUp = { gamepadManager.setVirtualButton(VirtualButton.N64_C_LEFT, false) }
                )
                // C-Right
                N64CButton(
                    symbol = "▶",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onDown = { gamepadManager.setVirtualButton(VirtualButton.N64_C_RIGHT, true); audio.triggerVibration(20, 100) },
                    onUp = { gamepadManager.setVirtualButton(VirtualButton.N64_C_RIGHT, false) }
                )
            }

            // A Button (Large Green button, Bottom Right)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-4).dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(N64Green)
                    .border(2.dp, Color(0xFF66BB6A), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                gamepadManager.setVirtualButton(VirtualButton.N64_A, true)
                                audio.triggerVibration(30, 140)
                                tryAwaitRelease()
                                gamepadManager.setVirtualButton(VirtualButton.N64_A, false)
                            }
                        )
                    }
                    .testTag("btn_n64_a")
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }

            // B Button (Blue button, Left of A)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp, y = (-20).dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(N64Blue)
                    .border(2.dp, Color(0xFF42A5F5), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                gamepadManager.setVirtualButton(VirtualButton.N64_B, true)
                                audio.triggerVibration(30, 140)
                                tryAwaitRelease()
                                gamepadManager.setVirtualButton(VirtualButton.N64_B, false)
                            }
                        )
                    }
                    .testTag("btn_n64_b")
            ) {
                Text("B", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun N64BumperButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 60.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2F38))
            .border(1.5.dp, Color(0xFF4B5162), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
            .testTag("btn_n64_$label")
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun N64ZTriggerButton(
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 84.dp, height = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF383C49))
            .border(2.dp, Color(0xFF61687E), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
            .testTag("btn_n64_z")
    ) {
        Text("[ Z TRIGGER ]", color = Color(0xFFD6DBEC), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun N64CButton(
    symbol: String,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(N64Yellow)
            .border(1.dp, Color(0xFFFFE082), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
    ) {
        Text(symbol, color = Color(0xFF261D00), fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
