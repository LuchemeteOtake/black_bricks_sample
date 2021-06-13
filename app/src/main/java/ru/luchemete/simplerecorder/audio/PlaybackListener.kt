package ru.luchemete.simplerecorder.audio

interface PlaybackListener {
    fun onProgress(progress: Int)
    fun onCompletion()
}