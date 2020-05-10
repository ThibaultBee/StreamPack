package com.github.thibaultbee.srtstreamer.app.ui.main

import android.media.MediaFormat
import android.util.Size
import androidx.lifecycle.ViewModel
import com.github.thibaultbee.srtstreamer.Streamer
import com.github.thibaultbee.srtstreamer.utils.Logger

class MainViewModel : ViewModel() {
    val streamer: Streamer by lazy {
        Streamer(Logger())
    }
}
