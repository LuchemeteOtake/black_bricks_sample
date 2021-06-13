package ru.luchemete.simplerecorder.audio.impl

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import ru.luchemete.simplerecorder.MainApp
import ru.luchemete.simplerecorder.audio.AudioDataReceivedListener
import ru.luchemete.simplerecorder.audio.Recorder


class RecorderImpl : Recorder {

    private var recorderThread: Thread? = null
    private var recording = false

    private var audioDataReceivedListener: AudioDataReceivedListener? = null

    private var bufferSize = AudioRecord.getMinBufferSize(
        MainApp.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    override fun setAudioDataReceivedListener(listener: AudioDataReceivedListener) {
        audioDataReceivedListener = listener
    }

    override fun startRecording() {
        if (recorderThread != null) return

        recording = true
        recorderThread = Thread { record() }
        recorderThread!!.start()
    }

    override fun stopRecording() {
        if (recorderThread == null) return

        recording = false
        recorderThread = null
    }

    private fun record() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            MainApp.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return
        }
        audioRecord.startRecording()
        while (recording) {
            val audioBuffer = ShortArray(bufferSize)
            audioRecord.read(audioBuffer, 0, bufferSize)
            audioDataReceivedListener?.onAudioDataReceived(audioBuffer)
        }
        audioRecord.stop()
        audioRecord.release()
    }
}