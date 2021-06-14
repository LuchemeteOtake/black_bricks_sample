package ru.luchemete.simplerecorder.audio

import java.util.*

interface Player {

    fun setSettings(settings: PlayBackSettings)

    fun setAudioData(audioData: LinkedList<ShortArray>)

    fun setPlayBackListener(listener: PlaybackListener)

    fun play()

    fun stop()

    fun setLooped(looped: Boolean)

    fun setCursorPosition(position: Int)
}