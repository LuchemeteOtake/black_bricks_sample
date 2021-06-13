package ru.luchemete.simplerecorder.audio

interface AudioDataReceivedListener {
    fun onAudioDataReceived(data: ShortArray)
}