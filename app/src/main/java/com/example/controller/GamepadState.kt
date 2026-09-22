package com.example.controller

data class GamepadState(
    // Controller metadata
    val isControllerConnected: Boolean = false,
    val controllerName: String = "Nenhum controle",
    val controllerType: String = "Desconectado", // Bluetooth / USB / Virtual

    // Analog Sticks (-1.0f to 1.0f)
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    // Triggers (0.0f to 1.0f)
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,

    // D-Pad
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,

    // PS1 Action Buttons
    val ps1Cross: Boolean = false,    // ✕
    val ps1Circle: Boolean = false,   // ○
    val ps1Square: Boolean = false,   // □
    val ps1Triangle: Boolean = false, // △

    // N64 Action Buttons
    val n64A: Boolean = false,
    val n64B: Boolean = false,
    val n64Z: Boolean = false,
    val n64CUp: Boolean = false,
    val n64CDown: Boolean = false,
    val n64CLeft: Boolean = false,
    val n64CRight: Boolean = false,

    // Shoulder Buttons
    val l1: Boolean = false,
    val r1: Boolean = false,
    val l2: Boolean = false,
    val r2: Boolean = false,

    // Center Buttons
    val start: Boolean = false,
    val select: Boolean = false,

    // Live debug / diagnostic info
    val lastEventSummary: String = "Aguardando entrada...",
    val rawKeyCodesPressed: Set<Int> = emptySet()
)
