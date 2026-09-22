package com.example.data.model

enum class ConsoleType(val displayName: String, val badge: String, val fileExtensions: List<String>) {
    PS1("PlayStation 1", "PS1", listOf("iso", "bin", "cue", "pbp", "img")),
    N64("Nintendo 64", "N64", listOf("z64", "n64", "v64", "rom"))
}

enum class AspectRatioMode(val label: String, val ratio: Float) {
    ORIGINAL_4_3("4:3 Original", 4f / 3f),
    WIDESCREEN_16_9("16:9 Wide", 16f / 9f),
    STRETCH("Preencher", 0f)
}

enum class ResolutionScale(val label: String, val factor: Int) {
    SCALE_1X("Nativo (240p/480i)", 1),
    SCALE_2X("HD 2x (480p)", 2),
    SCALE_3X("Full HD 3x (720p)", 3)
}
