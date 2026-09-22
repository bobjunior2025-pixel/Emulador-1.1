package com.example.controller

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.data.model.ButtonMapping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class GamepadManager(private val context: Context) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val _gamepadState = MutableStateFlow(GamepadState())
    val gamepadState: StateFlow<GamepadState> = _gamepadState.asStateFlow()

    private val activeKeyCodes = mutableSetOf<Int>()
    var deadzone: Float = 0.15f
    var buttonMapping: ButtonMapping = ButtonMapping()

    init {
        inputManager?.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        refreshConnectedControllers()
    }

    fun release() {
        inputManager?.unregisterInputDeviceListener(this)
    }

    fun refreshConnectedControllers() {
        val deviceIds = inputManager?.inputDeviceIds ?: IntArray(0)
        var foundGamepad: InputDevice? = null

        for (id in deviceIds) {
            val device = inputManager?.getInputDevice(id) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if ((isGamepad || isJoystick) && !device.isVirtual) {
                foundGamepad = device
                break
            }
        }

        if (foundGamepad != null) {
            val name = foundGamepad.name ?: "Controle Genérico"
            val isBluetooth = foundGamepad.sources and InputDevice.SOURCE_BLUETOOTH_STYLUS != 0 ||
                    !name.contains("usb", ignoreCase = true)
            _gamepadState.update {
                it.copy(
                    isControllerConnected = true,
                    controllerName = name,
                    controllerType = if (isBluetooth) "Bluetooth" else "USB Gamepad"
                )
            }
        } else {
            _gamepadState.update {
                it.copy(
                    isControllerConnected = false,
                    controllerName = "Nenhum controle físico",
                    controllerType = "Touch Virtual Ativo"
                )
            }
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshConnectedControllers()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshConnectedControllers()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshConnectedControllers()
    }

    // Process physical Key Events
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN

        if (isDown) {
            activeKeyCodes.add(keyCode)
        } else if (event.action == KeyEvent.ACTION_UP) {
            activeKeyCodes.remove(keyCode)
        }

        // Map keycodes to GamepadState
        val isCross = isDown && (keyCode == buttonMapping.ps1CrossKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        val isCircle = isDown && (keyCode == buttonMapping.ps1CircleKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_B)
        val isSquare = isDown && (keyCode == buttonMapping.ps1SquareKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_X)
        val isTriangle = isDown && (keyCode == buttonMapping.ps1TriangleKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_Y)

        val isL1 = isDown && (keyCode == buttonMapping.ps1L1KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L1)
        val isR1 = isDown && (keyCode == buttonMapping.ps1R1KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_R1)
        val isL2 = isDown && (keyCode == buttonMapping.ps1L2KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L2)
        val isR2 = isDown && (keyCode == buttonMapping.ps1R2KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_R2)

        val isStart = isDown && (keyCode == buttonMapping.ps1StartKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_MENU)
        val isSelect = isDown && (keyCode == buttonMapping.ps1SelectKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT || keyCode == KeyEvent.KEYCODE_BACK)

        val isDpadUp = isDown && keyCode == KeyEvent.KEYCODE_DPAD_UP
        val isDpadDown = isDown && keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        val isDpadLeft = isDown && keyCode == KeyEvent.KEYCODE_DPAD_LEFT
        val isDpadRight = isDown && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT

        val isN64A = isDown && (keyCode == buttonMapping.n64AKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_A)
        val isN64B = isDown && (keyCode == buttonMapping.n64BKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_B)
        val isN64Z = isDown && (keyCode == buttonMapping.n64ZKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L2 || keyCode == KeyEvent.KEYCODE_BUTTON_R2)

        val actionName = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "Botão A (Cross / N64 A)"
            KeyEvent.KEYCODE_BUTTON_B -> "Botão B (Circle / N64 B)"
            KeyEvent.KEYCODE_BUTTON_X -> "Botão X (Square)"
            KeyEvent.KEYCODE_BUTTON_Y -> "Botão Y (Triangle)"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1 Bumper"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1 Bumper"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2 / Z Trigger"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2 Trigger"
            KeyEvent.KEYCODE_BUTTON_START -> "Start"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
            KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Cima"
            KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Baixo"
            KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Esquerda"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Direita"
            else -> "Tecla #$keyCode"
        }

        _gamepadState.update { current ->
            current.copy(
                isControllerConnected = if (isGamepad) true else current.isControllerConnected,
                controllerName = if (isGamepad && event.device != null) event.device.name else current.controllerName,
                ps1Cross = if (keyCode == buttonMapping.ps1CrossKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_A) isDown else current.ps1Cross,
                ps1Circle = if (keyCode == buttonMapping.ps1CircleKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_B) isDown else current.ps1Circle,
                ps1Square = if (keyCode == buttonMapping.ps1SquareKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_X) isDown else current.ps1Square,
                ps1Triangle = if (keyCode == buttonMapping.ps1TriangleKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_Y) isDown else current.ps1Triangle,
                n64A = if (keyCode == buttonMapping.n64AKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_A) isDown else current.n64A,
                n64B = if (keyCode == buttonMapping.n64BKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_B) isDown else current.n64B,
                n64Z = if (keyCode == buttonMapping.n64ZKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L2) isDown else current.n64Z,
                l1 = if (keyCode == buttonMapping.ps1L1KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L1) isDown else current.l1,
                r1 = if (keyCode == buttonMapping.ps1R1KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_R1) isDown else current.r1,
                l2 = if (keyCode == buttonMapping.ps1L2KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_L2) isDown else current.l2,
                r2 = if (keyCode == buttonMapping.ps1R2KeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_R2) isDown else current.r2,
                start = if (keyCode == buttonMapping.ps1StartKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_START) isDown else current.start,
                select = if (keyCode == buttonMapping.ps1SelectKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) isDown else current.select,
                dpadUp = if (keyCode == KeyEvent.KEYCODE_DPAD_UP) isDown else current.dpadUp,
                dpadDown = if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) isDown else current.dpadDown,
                dpadLeft = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) isDown else current.dpadLeft,
                dpadRight = if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) isDown else current.dpadRight,
                lastEventSummary = "$actionName (${if (isDown) "Pressionado" else "Solto"})",
                rawKeyCodesPressed = activeKeyCodes.toSet()
            )
        }

        return isGamepad || keyCode in listOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
        )
    }

    // Process physical Motion Events (Analog Joysticks and Triggers)
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (event.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
        ) {
            return false
        }

        var lx = event.getAxisValue(MotionEvent.AXIS_X)
        var ly = event.getAxisValue(MotionEvent.AXIS_Y)
        var rx = event.getAxisValue(MotionEvent.AXIS_Z)
        var ry = event.getAxisValue(MotionEvent.AXIS_RZ)

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val lTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_GAS))

        // Deadzone filter
        if (abs(lx) < deadzone) lx = 0f
        if (abs(ly) < deadzone) ly = 0f
        if (abs(rx) < deadzone) rx = 0f
        if (abs(ry) < deadzone) ry = 0f

        val dpadUp = hatY < -0.5f
        val dpadDown = hatY > 0.5f
        val dpadLeft = hatX < -0.5f
        val dpadRight = hatX > 0.5f

        // N64 C-Buttons mapped to right analog stick
        val cUp = ry < -0.6f
        val cDown = ry > 0.6f
        val cLeft = rx < -0.6f
        val cRight = rx > 0.6f

        _gamepadState.update { current ->
            current.copy(
                isControllerConnected = true,
                controllerName = event.device?.name ?: current.controllerName,
                leftStickX = lx,
                leftStickY = ly,
                rightStickX = rx,
                rightStickY = ry,
                leftTrigger = lTrigger,
                rightTrigger = rTrigger,
                dpadUp = if (hatY != 0f) dpadUp else current.dpadUp,
                dpadDown = if (hatY != 0f) dpadDown else current.dpadDown,
                dpadLeft = if (hatX != 0f) dpadLeft else current.dpadLeft,
                dpadRight = if (hatX != 0f) dpadRight else current.dpadRight,
                n64CUp = cUp,
                n64CDown = cDown,
                n64CLeft = cLeft,
                n64CRight = cRight,
                n64Z = lTrigger > 0.5f || current.n64Z,
                lastEventSummary = "Analógico: LX=%.2f LY=%.2f | RX=%.2f RY=%.2f".format(lx, ly, rx, ry)
            )
        }
        return true
    }

    // Touch Virtual Control injection
    fun setVirtualStick(x: Float, y: Float) {
        var filteredX = x
        var filteredY = y
        if (abs(filteredX) < deadzone) filteredX = 0f
        if (abs(filteredY) < deadzone) filteredY = 0f

        _gamepadState.update {
            it.copy(leftStickX = filteredX, leftStickY = filteredY)
        }
    }

    fun setVirtualButton(action: VirtualButton, isPressed: Boolean) {
        _gamepadState.update { current ->
            when (action) {
                VirtualButton.PS1_CROSS -> current.copy(ps1Cross = isPressed)
                VirtualButton.PS1_CIRCLE -> current.copy(ps1Circle = isPressed)
                VirtualButton.PS1_SQUARE -> current.copy(ps1Square = isPressed)
                VirtualButton.PS1_TRIANGLE -> current.copy(ps1Triangle = isPressed)
                VirtualButton.N64_A -> current.copy(n64A = isPressed)
                VirtualButton.N64_B -> current.copy(n64B = isPressed)
                VirtualButton.N64_Z -> current.copy(n64Z = isPressed)
                VirtualButton.N64_C_UP -> current.copy(n64CUp = isPressed)
                VirtualButton.N64_C_DOWN -> current.copy(n64CDown = isPressed)
                VirtualButton.N64_C_LEFT -> current.copy(n64CLeft = isPressed)
                VirtualButton.N64_C_RIGHT -> current.copy(n64CRight = isPressed)
                VirtualButton.L1 -> current.copy(l1 = isPressed)
                VirtualButton.R1 -> current.copy(r1 = isPressed)
                VirtualButton.L2 -> current.copy(l2 = isPressed)
                VirtualButton.R2 -> current.copy(r2 = isPressed)
                VirtualButton.DPAD_UP -> current.copy(dpadUp = isPressed)
                VirtualButton.DPAD_DOWN -> current.copy(dpadDown = isPressed)
                VirtualButton.DPAD_LEFT -> current.copy(dpadLeft = isPressed)
                VirtualButton.DPAD_RIGHT -> current.copy(dpadRight = isPressed)
                VirtualButton.START -> current.copy(start = isPressed)
                VirtualButton.SELECT -> current.copy(select = isPressed)
            }
        }
    }

    fun resetVirtualInputs() {
        _gamepadState.update {
            it.copy(
                leftStickX = 0f,
                leftStickY = 0f,
                ps1Cross = false,
                ps1Circle = false,
                ps1Square = false,
                ps1Triangle = false,
                n64A = false,
                n64B = false,
                n64Z = false,
                n64CUp = false,
                n64CDown = false,
                n64CLeft = false,
                n64CRight = false,
                dpadUp = false,
                dpadDown = false,
                dpadLeft = false,
                dpadRight = false,
                l1 = false,
                r1 = false,
                l2 = false,
                r2 = false,
                start = false,
                select = false
            )
        }
    }
}

enum class VirtualButton {
    PS1_CROSS, PS1_CIRCLE, PS1_SQUARE, PS1_TRIANGLE,
    N64_A, N64_B, N64_Z, N64_C_UP, N64_C_DOWN, N64_C_LEFT, N64_C_RIGHT,
    L1, R1, L2, R2, DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    START, SELECT
}
