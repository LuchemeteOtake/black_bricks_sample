package ru.luchemete.simplerecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.luchemete.simplerecorder.R
import ru.luchemete.simplerecorder.audio.PlayBackSettings
import ru.luchemete.simplerecorder.databinding.RecorderActivityBinding
import ru.luchemete.simplerecorder.ui.RecorderViewModel.State

class RecorderActivity : AppCompatActivity() {

    private val viewModel by viewModel<RecorderViewModel>()

    private lateinit var binding: RecorderActivityBinding

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.startRecording()
            }
        }

    private lateinit var settingsDialog: AlertDialog

    private lateinit var lpf: CheckBox
    private lateinit var lpfValue: Slider

    private lateinit var gain: CheckBox
    private lateinit var gainLevel: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RecorderActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initSettingsDialog()
        initControls()
        initViewModel()
    }

    private fun initSettingsDialog() {
        settingsDialog = AlertDialog.Builder(this).apply {
            setTitle(R.string.settings_title)
            setView(getSettingsView())
            setPositiveButton(
                R.string.ok
            ) { _, _ ->
                getSettings()
            }
            setNegativeButton(
                R.string.cancel
            ) { _, _ ->
            }
        }.create()
    }

    private fun getSettingsView(): View {
        val view = View.inflate(this@RecorderActivity, R.layout.settings_dialog, null)
        view.apply {
            lpf = findViewById(R.id.lpf)
            lpfValue = findViewById(R.id.lpf_value)

            gain = findViewById(R.id.gain)
            gainLevel = findViewById(R.id.gain_level)
        }
        return view
    }

    private fun getSettings() {
        val lpfEnabled = lpf.isChecked
        val lpfValue = lpfValue.value

        val gainEnabled = gain.isChecked
        val gainLevel = gainLevel.value

        val settings = PlayBackSettings(lpfEnabled, lpfValue, gainEnabled, gainLevel)
        viewModel.setSettings(settings)
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
            val lengthString = String.format("%02d:%02d", length / 60, length % 60)

            val passed = it.passed
            val passedString = String.format("%02d:%02d", passed / 60, passed % 60)

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

        binding.settings.setOnClickListener {
            showSettings()
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

    private fun showSettings() {
        settingsDialog.show()
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
}