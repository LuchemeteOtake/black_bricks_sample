package ru.luchemete.simplerecorder.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.luchemete.simplerecorder.MainApp
import ru.luchemete.simplerecorder.audio.*
import java.util.*

class RecorderViewModel(
    private val recorder: Recorder,
    private val player: Player
) : ViewModel() {

    enum class State {
        PLAYING,
        RECORDING,
        IDLE,
    }

    private val audioData = LinkedList<ShortArray>()

    private var cursorPosition = 0
    private var recordingAt = 0
    private var state = State.IDLE
    private var isLooped = false

    val audioLiveData = MutableLiveData<SingleRecord>()

    val timerLiveData =
        MutableLiveData<PlaybackTimer>().apply {
            postValue(
                PlaybackTimer(
                    getLength(),
                    getPassedTime(cursorPosition)
                )
            )
        }

    val positionLiveData = MutableLiveData<Int>().apply { postValue(cursorPosition) }

    val stateLiveData = MutableLiveData<State>().apply { postValue(State.IDLE) }

    init {
        player.setAudioData(audioData)
        player.setPlayBackListener(object : PlaybackListener {
            override fun onProgress(progress: Int) {
                positionLiveData.postValue(progress)
                timerLiveData.postValue(PlaybackTimer(getLength(), getPassedTime(progress)))
            }

            override fun onCompletion() {
                positionLiveData.postValue(cursorPosition)
                timerLiveData.postValue(PlaybackTimer(getLength(), getPassedTime(cursorPosition)))
                player.setCursorPosition(cursorPosition)
                updateState(State.IDLE)
            }
        })

        recorder.setAudioDataReceivedListener(object : AudioDataReceivedListener {
            override fun onAudioDataReceived(data: ShortArray) {
                try {
                    audioData.removeAt(recordingAt)
                } catch (e: Exception) {
                }
                audioData.add(recordingAt, data)
                audioLiveData.postValue(SingleRecord(data, recordingAt, state == State.RECORDING))
                recordingAt++

                val passed =
                    if (state == State.RECORDING) {
                        getPassedTime(recordingAt)
                    } else {
                        getPassedTime(cursorPosition)
                    }
                timerLiveData.postValue(PlaybackTimer(getLength(), passed))
            }
        })
    }

    private fun getLength(): Int {
        if (audioData.isEmpty()) return 0

        return audioData[0].size * audioData.size / MainApp.SAMPLE_RATE
    }

    private fun getPassedTime(position: Int): Int {
        if (audioData.isEmpty()) return 0

        return getLength() * position / audioData.size
    }


    fun startRecording() {
        recordingAt = cursorPosition
        recorder.startRecording()
        updateState(State.RECORDING)
    }

    fun stopRecording() {
        recorder.stopRecording()
        positionLiveData.postValue(cursorPosition)
        timerLiveData.postValue(PlaybackTimer(getLength(), getPassedTime(cursorPosition)))
        updateState(State.IDLE)
    }

    fun play() {
        player.play()
        updateState(State.PLAYING)
    }

    fun pause() {
        player.stop()
        updateState(State.IDLE)
    }

    fun rewind() {
        setCursorPosition(0)
        positionLiveData.postValue(cursorPosition)
        timerLiveData.postValue(PlaybackTimer(getLength(), getPassedTime(cursorPosition)))
    }

    fun setCursorPosition(position: Int) {
        player.setCursorPosition(position)
        cursorPosition = position
    }

    fun setLooped(looped: Boolean) {
        player.setLooped(looped)
        isLooped = looped
        updateState(state)
    }

    fun setSettings(settings: PlayBackSettings) {
        player.setSettings(settings)
    }

    fun isLoopEnabled() = isLooped

    private fun updateState(nextState: State) {
        state = nextState
        stateLiveData.postValue(state)
    }

    override fun onCleared() {
        super.onCleared()

        recorder.stopRecording()
        player.stop()
    }
}