package ru.luchemete.simplerecorder.koin

import org.koin.dsl.module
import ru.luchemete.simplerecorder.audio.*
import ru.luchemete.simplerecorder.audio.impl.PlayerImpl
import ru.luchemete.simplerecorder.audio.impl.RecorderImpl

val audioModule = module {
    single<Recorder> { RecorderImpl() }

    single<Player> { PlayerImpl() }
}