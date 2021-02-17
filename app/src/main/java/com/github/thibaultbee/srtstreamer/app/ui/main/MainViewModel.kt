package com.github.thibaultbee.srtstreamer.app.ui.main

import android.content.Context
import android.media.AudioFormat
import android.media.MediaFormat
import android.util.Size
import androidx.lifecycle.ViewModel
import com.github.thibaultbee.srtstreamer.CaptureEncodeMuxTransmitPipeline
import com.github.thibaultbee.srtstreamer.data.AudioConfig
import com.github.thibaultbee.srtstreamer.data.VideoConfig
import com.github.thibaultbee.srtstreamer.endpoints.SrtProducer
import com.github.thibaultbee.srtstreamer.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.utils.Logger

class MainViewModel : ViewModel() {
    private val logger = Logger()
    val endpoint: SrtProducer by lazy {
        SrtProducer(logger)
    }

    private val tsServiceInfo = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "MyService",
        "MyProvider"
    )

    lateinit var captureEncodeMuxTransmitPipeline: CaptureEncodeMuxTransmitPipeline

    val videoConfig: VideoConfig by lazy {
        VideoConfig(
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
            startBitrate = 2000000,
            resolution = Size(1280, 720),
            fps = 30
        )
    }

    val audioConfig: AudioConfig by lazy {
        AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            startBitrate = 128000,
            sampleRate = 48000,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            audioByteFormat = AudioFormat.ENCODING_PCM_16BIT
        )
    }

    fun buildStreamer(context: Context) {
        captureEncodeMuxTransmitPipeline =
            CaptureEncodeMuxTransmitPipeline(context, tsServiceInfo, endpoint, logger)
    }
}
