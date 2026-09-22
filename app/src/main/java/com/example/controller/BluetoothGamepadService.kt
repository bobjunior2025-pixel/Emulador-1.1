package com.example.controller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Service class responsible for:
 * 1. Detecting connected Bluetooth controllers (via BluetoothProfile & InputManager).
 * 2. Listening to Bluetooth hardware connection/disconnection broadcasts.
 * 3. Mapping raw hardware events (KeyEvent & MotionEvent) into structured [EmulatorCommand]s.
 */
class BluetoothGamepadService(private val context: Context) : InputManager.InputDeviceListener {

    private val tag = "BluetoothGamepadService"
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // Connected devices state
    private val _connectedDevices = MutableStateFlow<List<BluetoothGamepadDevice>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothGamepadDevice>> = _connectedDevices.asStateFlow()

    // Stream of translated emulator commands for active emulation sessions
    private val _commandFlow = MutableSharedFlow<EmulatorCommand>(extraBufferCapacity = 64)
    val commandFlow: SharedFlow<EmulatorCommand> = _commandFlow.asSharedFlow()

    // Configurable analog deadzone
    var deadzone: Float = 0.15f

    // Broadcast receiver for Bluetooth state changes
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshConnectedDevices()
                }
            }
        }
    }

    private var isReceiverRegistered = false

    init {
        inputManager?.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        registerBluetoothReceiver()
        refreshConnectedDevices()
    }

    fun release() {
        inputManager?.unregisterInputDeviceListener(this)
        unregisterBluetoothReceiver()
    }

    private fun registerBluetoothReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            try {
                context.registerReceiver(bluetoothReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.w(tag, "Failed to register Bluetooth broadcast receiver", e)
            }
        }
    }

    private fun unregisterBluetoothReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.w(tag, "Failed to unregister Bluetooth broadcast receiver", e)
            }
        }
    }

    /**
     * Inspects both Android InputManager (for paired gamepad input devices)
     * and BluetoothAdapter (for paired Bluetooth controllers).
     */
    @SuppressLint("MissingPermission")
    fun refreshConnectedDevices() {
        val detectedList = mutableListOf<BluetoothGamepadDevice>()

        // 1. Query physical InputDevice gamepads (active hardware controllers)
        val deviceIds = inputManager?.inputDeviceIds ?: IntArray(0)
        for (id in deviceIds) {
            val device = inputManager?.getInputDevice(id) ?: continue
            if (device.isVirtual) continue

            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad || isJoystick) {
                val name = device.name ?: "Controle Genérico"
                val isBt = !name.contains("usb", ignoreCase = true)
                detectedList.add(
                    BluetoothGamepadDevice(
                        name = name,
                        address = "InputDevice #$id",
                        isConnected = true,
                        isRecognizedGamepad = true,
                        deviceCategory = categorizeDevice(name)
                    )
                )
            }
        }

        // 2. Query bonded Bluetooth devices if permission is granted
        val hasBtPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        if (hasBtPermission && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            try {
                val bonded = bluetoothAdapter.bondedDevices
                if (bonded != null) {
                    for (btDevice in bonded) {
                        val name = btDevice.name ?: "Dispositivo Bluetooth"
                        val isGamepad = isLikelyGamepadName(name)
                        // If not already in detected list by InputManager
                        if (detectedList.none { it.name.equals(name, ignoreCase = true) }) {
                            detectedList.add(
                                BluetoothGamepadDevice(
                                    name = name,
                                    address = btDevice.address,
                                    isConnected = false, // Paired, but maybe sleeping
                                    isRecognizedGamepad = isGamepad,
                                    deviceCategory = categorizeDevice(name)
                                )
                            )
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(tag, "Bluetooth permission not granted for bonded devices query", e)
            }
        }

        _connectedDevices.value = detectedList
    }

    private fun isLikelyGamepadName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("controller") || lower.contains("gamepad") ||
                lower.contains("wireless") || lower.contains("xbox") ||
                lower.contains("dualshock") || lower.contains("dualsense") ||
                lower.contains("joy-con") || lower.contains("switch") ||
                lower.contains("8bitdo") || lower.contains("pro") ||
                lower.contains("ipega") || lower.contains("gamesir")
    }

    private fun categorizeDevice(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("dualsense") -> "PlayStation DualSense (PS5)"
            lower.contains("dualshock") || lower.contains("wireless controller") -> "PlayStation DualShock (PS4)"
            lower.contains("xbox") -> "Xbox Wireless Controller"
            lower.contains("joy-con") -> "Nintendo Joy-Con"
            lower.contains("switch") || lower.contains("pro controller") -> "Nintendo Switch Pro"
            lower.contains("8bitdo") -> "8BitDo Retro Gamepad"
            lower.contains("gamesir") -> "GameSir Controller"
            else -> "Controle Bluetooth Padrão HID"
        }
    }

    // InputManager.InputDeviceListener callbacks
    override fun onInputDeviceAdded(deviceId: Int) {
        refreshConnectedDevices()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshConnectedDevices()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshConnectedDevices()
    }

    /**
     * Translates raw Android KeyEvents from the Bluetooth gamepad into high-level [EmulatorCommand]s.
     * Returns true if the key event was recognized and handled.
     */
    fun processKeyEvent(event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

        val keyCode = event.keyCode
        val isPressed = event.action == KeyEvent.ACTION_DOWN

        val button: EmulatorButton? = when (keyCode) {
            // Primary Action Buttons (Cross / A)
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER -> EmulatorButton.PRIMARY_ACTION
            // Secondary Action Buttons (Circle / B)
            KeyEvent.KEYCODE_BUTTON_B -> EmulatorButton.SECONDARY_ACTION
            // Tertiary Action Buttons (Square / X)
            KeyEvent.KEYCODE_BUTTON_X -> EmulatorButton.TERTIARY_ACTION
            // Quaternary Action Buttons (Triangle / Y)
            KeyEvent.KEYCODE_BUTTON_Y -> EmulatorButton.QUATERNARY_ACTION

            // Shoulder & Triggers
            KeyEvent.KEYCODE_BUTTON_L1 -> EmulatorButton.L1
            KeyEvent.KEYCODE_BUTTON_R1 -> EmulatorButton.R1
            KeyEvent.KEYCODE_BUTTON_L2 -> EmulatorButton.L2
            KeyEvent.KEYCODE_BUTTON_R2 -> EmulatorButton.R2

            // Menu / System
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> EmulatorButton.START
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> EmulatorButton.SELECT
            KeyEvent.KEYCODE_BUTTON_MODE -> EmulatorButton.HOME

            // D-Pad Buttons
            KeyEvent.KEYCODE_DPAD_UP -> {
                emitCommand(EmulatorCommand.DirectionalInput(0f, if (isPressed) -1f else 0f, InputSource.DPAD))
                null
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                emitCommand(EmulatorCommand.DirectionalInput(0f, if (isPressed) 1f else 0f, InputSource.DPAD))
                null
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                emitCommand(EmulatorCommand.DirectionalInput(if (isPressed) -1f else 0f, 0f, InputSource.DPAD))
                null
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                emitCommand(EmulatorCommand.DirectionalInput(if (isPressed) 1f else 0f, 0f, InputSource.DPAD))
                null
            }

            // N64 C-Buttons (if hardware reports dedicated buttons)
            KeyEvent.KEYCODE_BUTTON_C -> EmulatorButton.C_RIGHT
            KeyEvent.KEYCODE_BUTTON_Z -> EmulatorButton.L2

            else -> null
        }

        if (button != null) {
            emitCommand(EmulatorCommand.ButtonAction(button, isPressed))
            return true
        }

        return isGamepad
    }

    /**
     * Translates raw Android MotionEvents (Thumbsticks, D-Pad Hat, Triggers) into [EmulatorCommand]s.
     * Returns true if the motion event was handled.
     */
    fun processMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (event.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
        ) {
            return false
        }

        // Left Analog Stick
        var lx = event.getAxisValue(MotionEvent.AXIS_X)
        var ly = event.getAxisValue(MotionEvent.AXIS_Y)
        if (abs(lx) < deadzone) lx = 0f
        if (abs(ly) < deadzone) ly = 0f
        emitCommand(EmulatorCommand.DirectionalInput(lx, ly, InputSource.LEFT_STICK))

        // Right Analog Stick
        var rx = event.getAxisValue(MotionEvent.AXIS_Z)
        var ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (abs(rx) < deadzone) rx = 0f
        if (abs(ry) < deadzone) ry = 0f
        emitCommand(EmulatorCommand.DirectionalInput(rx, ry, InputSource.RIGHT_STICK))

        // D-Pad Hat (POV hat on many Bluetooth gamepads)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        emitCommand(EmulatorCommand.DirectionalInput(hatX, hatY, InputSource.DPAD))

        // Analog Triggers (L2 / R2)
        val lTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_GAS))

        emitCommand(EmulatorCommand.ButtonAction(EmulatorButton.L2, lTrigger > 0.5f))
        emitCommand(EmulatorCommand.ButtonAction(EmulatorButton.R2, rTrigger > 0.5f))

        return true
    }

    private fun emitCommand(command: EmulatorCommand) {
        serviceScope.launch {
            _commandFlow.emit(command)
        }
    }
}
