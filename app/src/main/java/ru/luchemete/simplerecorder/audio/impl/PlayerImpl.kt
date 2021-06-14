package ru.luchemete.simplerecorder.audio.impl

import android.media.*
import org.jtransforms.fft.FloatFFT_1D
import ru.luchemete.simplerecorder.MainApp
import ru.luchemete.simplerecorder.audio.PlayBackSettings
import ru.luchemete.simplerecorder.audio.PlaybackListener
import ru.luchemete.simplerecorder.audio.Player
import uk.me.berndporr.iirj.Butterworth
import java.util.*
import kotlin.math.pow


class PlayerImpl : Player {

    private var playerThread: Thread? = null
    private var playing = false

    private var playbackListener: PlaybackListener? = null

    private var audioData = LinkedList<FloatArray>()
    private lateinit var processedAudioData: List<FloatArray>

    private var cursorPosition = 0

    private var looped = false

    private var settings = PlayBackSettings()
    var butterworth = Butterworth()

    private var audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var audioFormat = AudioFormat.Builder()
        .setEncoding(MainApp.ENCODING)
        .setSampleRate(MainApp.SAMPLE_RATE)
        .build()

    private var bufferSize = AudioTrack.getMinBufferSize(
        MainApp.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
        MainApp.ENCODING
    )

    override fun setSettings(settings: PlayBackSettings) {
        this.settings = settings
    }

    override fun setAudioData(audioData: LinkedList<FloatArray>) {
        this.audioData = audioData
    }

    private fun processAudioData() {
        processedAudioData = audioData

        if (!settings.lpfEnabled && !settings.gainEnabled) {
            return
        }

        gainProcessor()
        lpfProcessor()
    }

    private fun gainProcessor() {
        if (!settings.gainEnabled) return

        val scale = 10f.pow(settings.gainLevel / 20f)

        processedAudioData = processedAudioData.map {
            it.map { value -> value * scale }.toFloatArray()
        }
    }

    private fun lpfProcessor() {
        if (!settings.lpfEnabled) return

        val freq = settings.lpfValue
        butterworth.lowPass(0, MainApp.SAMPLE_RATE.toDouble(), freq.toDouble())

        processedAudioData = processedAudioData.map {
            it.map { value -> butterworth.filter(value.toDouble()).toFloat() }.toFloatArray()

//            val audioBuffer = it.map { value -> value }.toFloatArray()
//            val FFT_SIZE = audioBuffer.size / 2
//            val mFFT = FloatFFT_1D(FFT_SIZE.toLong())
//
//            mFFT.realForward(audioBuffer)
//
//            for (fftBin in 0 until FFT_SIZE) {
//                val frequency = fftBin.toFloat() * 44100f / FFT_SIZE.toFloat()
//
//                if (frequency > settings.lpfValue) {
//                    val real = 2 * fftBin
//                    val imaginary = 2 * fftBin + 1
//
//                    audioBuffer[real] = 0f
//                    audioBuffer[imaginary] = 0f
//                }
//            }
//
//            mFFT.realInverse(audioBuffer, false)
//
//            audioBuffer
        }
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

        processAudioData()
        playing = true
        playerThread = Thread { startPlayback() }
        playerThread!!.start()
    }

    override fun stop() {
        stopPlayback()
    }

    private fun rewindIfNeeded() {
        if (looped && cursorPosition >= processedAudioData.size) {
            cursorPosition = 0
        }
    }

    private fun shouldPlay(): Boolean {
        return (looped || cursorPosition < processedAudioData.size) && playing
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
            val data = processedAudioData[cursorPosition]
            val numSamplesLeft: Int = data.size

            val samplesToWrite: Int = if (numSamplesLeft >= bufferSize) {
                bufferSize
            } else {
                numSamplesLeft
            }

            audioTrack.write(data, 0, samplesToWrite, AudioTrack.WRITE_BLOCKING)
            cursorPosition++
            rewindIfNeeded()
        }

        if (cursorPosition >= processedAudioData.size) {
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