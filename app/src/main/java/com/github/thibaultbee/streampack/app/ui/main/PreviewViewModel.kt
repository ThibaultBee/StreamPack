package com.github.thibaultbee.streampack.app.ui.main

import android.Manifest
import android.app.Application
import android.media.AudioFormat
import android.media.MediaFormat
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import com.github.thibaultbee.streampack.CaptureSrtLiveStream
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.Logger

class PreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = Logger()

    private val configuration = Configuration(getApplication())

    private val tsServiceInfo = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "MyService",
        "MyProvider"
    )

    lateinit var captureLiveStream: CaptureSrtLiveStream

    val cameraId: String
        get() = captureLiveStream.videoSource.cameraId

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun buildStreamer() {
        val videoConfig =
            VideoConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                startBitrate = configuration.video.bitrate * 1000, // to b/s
                resolution = configuration.video.resolution,
                fps = 30
            )

        val audioConfig = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            startBitrate = 128000,
            sampleRate = 48000,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            audioByteFormat = AudioFormat.ENCODING_PCM_16BIT
        )
        captureLiveStream =
            CaptureSrtLiveStream(getApplication(), tsServiceInfo, logger)
        captureLiveStream.configure(audioConfig, videoConfig)
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface) {
        captureLiveStream.startCapture(previewSurface)
    }

    fun stopCapture() {
        captureLiveStream.stopCapture()
    }

    fun startStream() {
        captureLiveStream.connect(configuration.connection.ip, configuration.connection.port)
        captureLiveStream.startStream()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        captureLiveStream.stopStream()
        captureLiveStream.disconnect()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        if (captureLiveStream.videoSource.cameraId == "0") {
            captureLiveStream.changeVideoSource("1")
        } else {
            captureLiveStream.changeVideoSource("0")
        }
    }

    override fun onCleared() {
        captureLiveStream.release()
    }
}
