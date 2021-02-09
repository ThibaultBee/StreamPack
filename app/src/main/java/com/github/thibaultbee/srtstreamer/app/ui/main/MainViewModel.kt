package com.github.thibaultbee.srtstreamer.app.ui.main

import androidx.lifecycle.ViewModel
import com.github.thibaultbee.srtstreamer.Streamer
import com.github.thibaultbee.srtstreamer.endpoints.SrtProducer
import com.github.thibaultbee.srtstreamer.utils.Logger

class MainViewModel : ViewModel() {
    private val logger = Logger()
    val endpoint: SrtProducer by lazy {
        SrtProducer(logger)
    }
    val streamer: Streamer by lazy {
        Streamer(endpoint, logger)
    }
}
