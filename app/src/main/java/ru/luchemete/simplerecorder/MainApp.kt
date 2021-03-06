package ru.luchemete.simplerecorder

import android.app.Application
import android.media.AudioFormat
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ru.luchemete.simplerecorder.koin.audioModule
import ru.luchemete.simplerecorder.koin.viewModelModule

class MainApp : Application() {

    companion object {
        val SAMPLE_RATE = 44100
        val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApp)
            modules(audioModule, viewModelModule)
        }
    }
}