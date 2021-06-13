package ru.luchemete.simplerecorder.koin

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.luchemete.simplerecorder.ui.RecorderViewModel

val viewModelModule = module {
    viewModel { RecorderViewModel(get(), get()) }
}