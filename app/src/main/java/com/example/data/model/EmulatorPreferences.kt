package com.example.data.model

data class EmulatorSettings(
    val scanlinesEnabled: Boolean = true,
    val scanlineIntensity: Float = 0.35f,
    val crtCurvature: Boolean = false,
    val ps1Dithering: Boolean = true,
    val resolutionScale: ResolutionScale = ResolutionScale.SCALE_2X,
    val aspectRatio: AspectRatioMode = AspectRatioMode.ORIGINAL_4_3,
    val audioEnabled: Boolean = true,
    val audioVolume: Float = 0.85f,
    val playPs1StartupChime: Boolean = true,
    val n64ExpansionPak: Boolean = true,
    val touchControlsOpacity: Float = 0.65f,
    val autoHideTouchWhenControllerConnected: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val analogDeadzone: Float = 0.15f
)

data class ButtonMapping(
    // Android KeyCode mapped to console actions
    val ps1CrossKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_A,
    val ps1CircleKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_B,
    val ps1SquareKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_X,
    val ps1TriangleKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_Y,
    val ps1L1KeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_L1,
    val ps1R1KeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_R1,
    val ps1L2KeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_L2,
    val ps1R2KeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_R2,
    val ps1SelectKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_SELECT,
    val ps1StartKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_START,

    // N64 mappings
    val n64AKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_A,
    val n64BKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_B,
    val n64ZKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_L2,
    val n64LKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_L1,
    val n64RKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_R1,
    val n64StartKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_START,
    val n64CUpKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_Y,
    val n64CDownKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_A,
    val n64CLeftKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_X,
    val n64CRightKeyCode: Int = android.view.KeyEvent.KEYCODE_BUTTON_B
)
