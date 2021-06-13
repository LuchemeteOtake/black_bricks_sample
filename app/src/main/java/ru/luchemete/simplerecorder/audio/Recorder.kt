package ru.luchemete.simplerecorder.audio

interface Recorder {

    fun setAudioDataReceivedListener(listener: AudioDataReceivedListener)

    fun startRecording()

    fun stopRecording()
}