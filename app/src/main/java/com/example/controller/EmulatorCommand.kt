package com.example.controller

/**
 * High-level Emulator Commands mapped from physical gamepad inputs.
 * These commands abstract console-specific and system-wide emulator actions.
 */
sealed class EmulatorCommand {
    // Directional & Movement Commands
    data class DirectionalInput(
        val x: Float, // -1.0f (left) to 1.0f (right)
        val y: Float, // -1.0f (up) to 1.0f (down)
        val source: InputSource
    ) : EmulatorCommand()

    // Action Commands
    data class ButtonAction(
        val button: EmulatorButton,
        val isPressed: Boolean
    ) : EmulatorCommand()

    // System / Hotkey Commands
    object TogglePause : EmulatorCommand()
    object FastForward : EmulatorCommand()
    object QuickSave : EmulatorCommand()
    object QuickLoad : EmulatorCommand()
    object ResetSession : EmulatorCommand()
}

enum class InputSource {
    LEFT_STICK,
    RIGHT_STICK,
    DPAD
}

enum class EmulatorButton {
    // Primary Action
    PRIMARY_ACTION,   // PS1 Cross / N64 A / Xbox A / Switch B
    SECONDARY_ACTION, // PS1 Circle / N64 B / Xbox B / Switch A
    TERTIARY_ACTION,  // PS1 Square / N64 C-Left / Xbox X / Switch Y
    QUATERNARY_ACTION,// PS1 Triangle / N64 C-Up / Xbox Y / Switch X

    // Shoulders & Triggers
    L1,
    R1,
    L2,               // or N64 Z-Trigger
    R2,

    // Center & Meta
    START,
    SELECT,
    HOME,

    // N64 C-Buttons
    C_UP,
    C_DOWN,
    C_LEFT,
    C_RIGHT
}

/**
 * Represents a detected Bluetooth device (controller or audio accessory).
 */
data class BluetoothGamepadDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val isRecognizedGamepad: Boolean,
    val deviceCategory: String // "DualShock/DualSense", "Xbox Controller", "Switch Pro", "8BitDo", "Generic Gamepad"
)
