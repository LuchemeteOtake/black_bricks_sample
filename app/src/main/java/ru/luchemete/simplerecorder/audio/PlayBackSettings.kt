package ru.luchemete.simplerecorder.audio

data class PlayBackSettings(
    val lpfEnabled: Boolean = false,
    val lpfValue: Float = 0.0f,
    val gainEnabled: Boolean = false,
    val gainLevel: Float = 0.0f
)
