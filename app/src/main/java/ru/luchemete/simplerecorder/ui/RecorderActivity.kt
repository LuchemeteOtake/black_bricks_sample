package ru.luchemete.simplerecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.luchemete.simplerecorder.databinding.RecorderActivityBinding
import ru.luchemete.simplerecorder.ui.RecorderViewModel.State
import java.util.*

class RecorderActivity : AppCompatActivity() {

    private val viewModel by viewModel<RecorderViewModel>()

    private lateinit var binding: RecorderActivityBinding

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.startRecording()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RecorderActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initControls()
        initViewModel()
    }

    private fun startAudioRecordingSafe() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startRecording()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Snackbar.make(
                    binding.visualizer, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("OK") {
                    requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    }

    private fun initViewModel() {
        viewModel.audioLiveData.observe(this) {
            binding.visualizer.addAmp(it.data, it.position, it.isRecording)
        }

        viewModel.stateLiveData.observe(this) {
            updateControls(it)
        }

        viewModel.positionLiveData.observe(this) {
            binding.visualizer.setCursorPosition(it)
        }

        viewModel.timerLiveData.observe(this) {
            val length = it.length
            val lengthString = String.format("%02d:%02d", length/60, length%60)

            val passed = it.passed
            val passedString = String.format("%02d:%02d", passed/60, passed%60)

            binding.playbackTimer.text = "$passedString/$lengthString"
        }

    }

    private fun initControls() {
        binding.visualizer.apply {
            onCursorPositionChanged = {
                viewModel.setCursorPosition(it)
            }
        }

        binding.rewindButtonContainer.rewind.setOnClickListener {
            viewModel.rewind()
        }

        binding.playButtonContainer.play.setOnClickListener {
            viewModel.play()
        }

        binding.playButtonContainer.pause.setOnClickListener {
            viewModel.pause()
        }

        binding.recordButtonContainer.startRecord.setOnClickListener {
            startAudioRecordingSafe()
        }

        binding.recordButtonContainer.stopRecord.setOnClickListener {
            viewModel.stopRecording()
        }

        binding.loopButtonContainer.loop.setOnClickListener {
            viewModel.setLooped(true)
        }

        binding.loopButtonContainer.loopActive.setOnClickListener {
            viewModel.setLooped(false)
        }
    }

    private fun updateControls(state: State) {
        binding.rewindButtonContainer.rewind.visibility = View.GONE
        binding.rewindButtonContainer.rewindDisabled.visibility = View.GONE

        binding.playButtonContainer.play.visibility = View.GONE
        binding.playButtonContainer.pause.visibility = View.GONE
        binding.playButtonContainer.playDisabled.visibility = View.GONE

        binding.recordButtonContainer.startRecord.visibility = View.GONE
        binding.recordButtonContainer.stopRecord.visibility = View.GONE

        binding.loopButtonContainer.loop.visibility = View.GONE
        binding.loopButtonContainer.loopActive.visibility = View.GONE
        binding.loopButtonContainer.loopDisabled.visibility = View.GONE

        when (state) {
            State.IDLE -> {
                binding.rewindButtonContainer.rewind.visibility = View.VISIBLE
                binding.playButtonContainer.play.visibility = View.VISIBLE
                binding.recordButtonContainer.startRecord.visibility = View.VISIBLE

                if (viewModel.isLoopEnabled()) {
                    binding.loopButtonContainer.loopActive.visibility = View.VISIBLE
                } else {
                    binding.loopButtonContainer.loop.visibility = View.VISIBLE
                }
            }
            State.PLAYING -> {
                binding.rewindButtonContainer.rewind.visibility = View.VISIBLE
                binding.playButtonContainer.pause.visibility = View.VISIBLE
                binding.recordButtonContainer.startRecord.visibility = View.VISIBLE

                if (viewModel.isLoopEnabled()) {
                    binding.loopButtonContainer.loopActive.visibility = View.VISIBLE
                } else {
                    binding.loopButtonContainer.loop.visibility = View.VISIBLE
                }
            }
            State.RECORDING -> {
                binding.rewindButtonContainer.rewindDisabled.visibility = View.VISIBLE
                binding.playButtonContainer.playDisabled.visibility = View.VISIBLE
                binding.recordButtonContainer.stopRecord.visibility = View.VISIBLE
                binding.loopButtonContainer.loopDisabled.visibility = View.VISIBLE
            }
        }
    }
}