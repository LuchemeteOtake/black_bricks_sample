package ru.luchemete.simplerecorder.audio.impl

import android.media.*
import ru.luchemete.simplerecorder.MainApp
import ru.luchemete.simplerecorder.audio.PlaybackListener
import ru.luchemete.simplerecorder.audio.Player
import java.util.*


class PlayerImpl : Player {

    private var playerThread: Thread? = null
    private var playing = false

    private var playbackListener: PlaybackListener? = null

    private var audioData = LinkedList<ShortArray>()
    private var cursorPosition = 0

    private var looped = false

    private var audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(MainApp.SAMPLE_RATE)
        .build()

    private var bufferSize = AudioTrack.getMinBufferSize(
        MainApp.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    override fun setAudioData(audioData: LinkedList<ShortArray>) {
        this.audioData = audioData
    }

    override fun setPlayBackListener(listener: PlaybackListener) {
        playbackListener = listener
    }

    override fun setLooped(looped: Boolean) {
        this.looped = looped
    }

    override fun setCursorPosition(position: Int) {
        cursorPosition = position
    }

    override fun play() {
        if (playerThread != null) return

        if (audioData.isEmpty()) return

        playing = true
        playerThread = Thread { startPlayback() }
        playerThread!!.start()
    }

    override fun stop() {
        stopPlayback()
    }

    private fun rewindIfNeeded() {
        if (looped && cursorPosition >= audioData.size) {
            cursorPosition = 0
        }
    }

    private fun shouldPlay(): Boolean {
        return (looped || cursorPosition < audioData.size) && playing
    }

    private fun startPlayback() {
        val audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack.setPlaybackPositionUpdateListener(object :
            AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack) {
                playbackListener?.onProgress(cursorPosition)
            }

            override fun onMarkerReached(track: AudioTrack) {}
        })
        audioTrack.positionNotificationPeriod = MainApp.SAMPLE_RATE / 60
        audioTrack.playbackParams = PlaybackParams().apply {
            pitch = 0.5f
            speed = 0.5f
        }
        audioTrack.play()

        while (shouldPlay()) {
            val data = audioData[cursorPosition]
            val numSamplesLeft: Int = data.size

            val samplesToWrite: Int = if (numSamplesLeft >= bufferSize) {
                bufferSize
            } else {
                numSamplesLeft
            }

            audioTrack.write(data, 0, samplesToWrite)
            cursorPosition++
            rewindIfNeeded()
        }

        if (cursorPosition >= audioData.size) {
            playbackListener?.onCompletion()
        }

        audioTrack.release()
        stopPlayback()
    }

    private fun stopPlayback() {
        if (playerThread == null) return

        playing = false
        playerThread = null
    }
}