package com.example.ui.controls

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadManager
import com.example.controller.VirtualButton
import com.example.ui.theme.PS1DarkGrey
import com.example.ui.theme.PS1Grey
import com.example.ui.theme.PS1SymbolCircle
import com.example.ui.theme.PS1SymbolCross
import com.example.ui.theme.PS1SymbolSquare
import com.example.ui.theme.PS1SymbolTriangle
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun PS1TouchControls(
    gamepadManager: GamepadManager,
    audio: RetroAudioSynthesizer,
    opacity: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    var analogMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(opacity)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Shoulder Triggers (L1/L2 and R1/R2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ps1TriggerButton("L2", { gamepadManager.setVirtualButton(VirtualButton.L2, true); audio.triggerVibration(25, 120) }, { gamepadManager.setVirtualButton(VirtualButton.L2, false) })
                Ps1TriggerButton("L1", { gamepadManager.setVirtualButton(VirtualButton.L1, true); audio.triggerVibration(25, 120) }, { gamepadManager.setVirtualButton(VirtualButton.L1, false) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Ps1TriggerButton("R1", { gamepadManager.setVirtualButton(VirtualButton.R1, true); audio.triggerVibration(25, 120) }, { gamepadManager.setVirtualButton(VirtualButton.R1, false) })
                Ps1TriggerButton("R2", { gamepadManager.setVirtualButton(VirtualButton.R2, true); audio.triggerVibration(25, 120) }, { gamepadManager.setVirtualButton(VirtualButton.R2, false) })
            }
        }

        // Center Select, Analog Mode, and Start
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ps1CenterButton("SELECT", { gamepadManager.setVirtualButton(VirtualButton.SELECT, true); audio.triggerVibration(30, 140) }, { gamepadManager.setVirtualButton(VirtualButton.SELECT, false) })

            // Analog Toggle Button (like original PS1 controller red LED)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF20232A))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            analogMode = !analogMode
                            audio.triggerVibration(40, 160)
                        })
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (analogMode) Color.Red else Color(0xFF400000))
                )
                Text("ANALOG", color = PS1Grey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Ps1CenterButton("START", { gamepadManager.setVirtualButton(VirtualButton.START, true); audio.triggerVibration(30, 140) }, { gamepadManager.setVirtualButton(VirtualButton.START, false) })
        }

        // Left Controls: D-Pad or Virtual Analog Stick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 20.dp)
                .size(150.dp)
        ) {
            if (analogMode) {
                VirtualAnalogStick(
                    onStickMoved = { x, y -> gamepadManager.setVirtualStick(x, y) },
                    onStickReleased = { gamepadManager.setVirtualStick(0f, 0f) }
                )
            } else {
                Ps1Dpad(
                    onDirectionPressed = { btn -> gamepadManager.setVirtualButton(btn, true); audio.triggerVibration(25, 120) },
                    onDirectionReleased = { btn -> gamepadManager.setVirtualButton(btn, false) }
                )
            }
        }

        // Right Controls: PS1 Action Buttons (△ ○ ✕ □)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 20.dp)
                .size(150.dp)
        ) {
            Ps1ActionCluster(
                onButtonPressed = { btn -> gamepadManager.setVirtualButton(btn, true); audio.triggerVibration(30, 140) },
                onButtonReleased = { btn -> gamepadManager.setVirtualButton(btn, false) }
            )
        }
    }
}

@Composable
private fun Ps1TriggerButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 54.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2D36))
            .border(1.5.dp, Color(0xFF404656), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
            .testTag("btn_ps1_$label")
    ) {
        Text(label, color = PS1Grey, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun Ps1CenterButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 62.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF22252D))
            .border(1.dp, Color(0xFF3B4150), RoundedCornerShape(12.dp))
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
        Text(label, color = PS1Grey, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

@Composable
private fun Ps1Dpad(
    onDirectionPressed: (VirtualButton) -> Unit,
    onDirectionReleased: (VirtualButton) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Cross background
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 136.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PS1DarkGrey)
        )
        Box(
            modifier = Modifier
                .size(width = 136.dp, height = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PS1DarkGrey)
        )

        // UP
        DpadArrow(
            label = "▲",
            modifier = Modifier.align(Alignment.TopCenter),
            onDown = { onDirectionPressed(VirtualButton.DPAD_UP) },
            onUp = { onDirectionReleased(VirtualButton.DPAD_UP) }
        )
        // DOWN
        DpadArrow(
            label = "▼",
            modifier = Modifier.align(Alignment.BottomCenter),
            onDown = { onDirectionPressed(VirtualButton.DPAD_DOWN) },
            onUp = { onDirectionReleased(VirtualButton.DPAD_DOWN) }
        )
        // LEFT
        DpadArrow(
            label = "◀",
            modifier = Modifier.align(Alignment.CenterStart),
            onDown = { onDirectionPressed(VirtualButton.DPAD_LEFT) },
            onUp = { onDirectionReleased(VirtualButton.DPAD_LEFT) }
        )
        // RIGHT
        DpadArrow(
            label = "▶",
            modifier = Modifier.align(Alignment.CenterEnd),
            onDown = { onDirectionPressed(VirtualButton.DPAD_RIGHT) },
            onUp = { onDirectionReleased(VirtualButton.DPAD_RIGHT) }
        )
    }
}

@Composable
private fun DpadArrow(
    label: String,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(46.dp)
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
        Text(label, color = PS1Grey, fontSize = 16.sp)
    }
}

@Composable
private fun Ps1ActionCluster(
    onButtonPressed: (VirtualButton) -> Unit,
    onButtonReleased: (VirtualButton) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Base plate circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2129))
        )

        // Triangle (Top)
        Ps1SymbolButton(
            symbol = "△",
            color = PS1SymbolTriangle,
            modifier = Modifier.align(Alignment.TopCenter),
            onDown = { onButtonPressed(VirtualButton.PS1_TRIANGLE) },
            onUp = { onButtonReleased(VirtualButton.PS1_TRIANGLE) }
        )
        // Circle (Right)
        Ps1SymbolButton(
            symbol = "○",
            color = PS1SymbolCircle,
            modifier = Modifier.align(Alignment.CenterEnd),
            onDown = { onButtonPressed(VirtualButton.PS1_CIRCLE) },
            onUp = { onButtonReleased(VirtualButton.PS1_CIRCLE) }
        )
        // Cross (Bottom)
        Ps1SymbolButton(
            symbol = "✕",
            color = PS1SymbolCross,
            modifier = Modifier.align(Alignment.BottomCenter),
            onDown = { onButtonPressed(VirtualButton.PS1_CROSS) },
            onUp = { onButtonReleased(VirtualButton.PS1_CROSS) }
        )
        // Square (Left)
        Ps1SymbolButton(
            symbol = "□",
            color = PS1SymbolSquare,
            modifier = Modifier.align(Alignment.CenterStart),
            onDown = { onButtonPressed(VirtualButton.PS1_SQUARE) },
            onUp = { onButtonReleased(VirtualButton.PS1_SQUARE) }
        )
    }
}

@Composable
private fun Ps1SymbolButton(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C2F3A))
            .border(1.5.dp, Color(0xFF434757), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
            .testTag("btn_ps1_$symbol")
    ) {
        Text(symbol, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun VirtualAnalogStick(
    onStickMoved: (Float, Float) -> Unit,
    onStickReleased: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 45f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(Color(0xFF1B1D24))
            .border(2.dp, Color(0xFF383D4E), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centered = offset - Offset(size.width / 2f, size.height / 2f)
                        val dist = sqrt(centered.x * centered.x + centered.y * centered.y)
                        val clamped = if (dist > maxRadius) centered * (maxRadius / dist) else centered
                        dragOffset = clamped
                        onStickMoved(clamped.x / maxRadius, clamped.y / maxRadius)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = dragOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val clamped = if (dist > maxRadius) newOffset * (maxRadius / dist) else newOffset
                        dragOffset = clamped
                        onStickMoved(clamped.x / maxRadius, clamped.y / maxRadius)
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onStickReleased()
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onStickReleased()
                    }
                )
            }
    ) {
        // Inner thumb nub
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF383C4A))
                .border(2.dp, Color(0xFF555B70), CircleShape)
        )
    }
}
