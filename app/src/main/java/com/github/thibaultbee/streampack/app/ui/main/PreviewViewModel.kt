/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.app.ui.main

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.thibaultbee.streampack.BaseCaptureStream
import com.github.thibaultbee.streampack.CaptureFileStream
import com.github.thibaultbee.streampack.CaptureSrtLiveStream
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.app.configuration.Configuration.Endpoint.EndpointType
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.Logger
import java.io.File

class PreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = this::class.java.simpleName

    private val logger = Logger()

    private val configuration = Configuration(getApplication())

    private lateinit var captureStream: BaseCaptureStream

    val cameraId: String
        get() = captureStream.videoSource.cameraId

    val error = MutableLiveData<String>()

    val streamRequiredPermissions: List<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (captureStream is CaptureFileStream) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return permissions
        }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun createStreamer() {
        val videoConfig =
            VideoConfig(
                mimeType = configuration.video.encoder,
                startBitrate = configuration.video.bitrate * 1000, // to b/s
                resolution = configuration.video.resolution,
                fps = configuration.video.fps
            )

        val audioConfig = AudioConfig(
            mimeType = configuration.audio.encoder,
            startBitrate = 128000,
            sampleRate = 48000,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            audioByteFormat = AudioFormat.ENCODING_PCM_16BIT
        )

        val tsServiceInfo = ServiceInfo(
            ServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

        try {
            captureStream = if (configuration.endpoint.enpointType == EndpointType.SRT) {
                CaptureSrtLiveStream(getApplication(), tsServiceInfo, logger)
            } else {
                CaptureFileStream(getApplication(), tsServiceInfo, logger)
            }

            captureStream.onErrorListener = object : OnErrorListener {
                override fun onError(source: String, message: String) {
                    error.postValue("$source: $message")
                }
            }

            if (captureStream is CaptureSrtLiveStream) {
                (captureStream as CaptureSrtLiveStream).onConnectionListener =
                    object : OnConnectionListener {
                        override fun onLost(message: String) {
                            error.postValue("Connection lost: $message")
                        }

                        override fun onFailed(message: String) {
                            error.postValue("Connection failed: $message")
                        }

                        override fun onSuccess() {
                            Log.i(tag, "Connection succeeded")
                        }
                    }
            }

            captureStream.configure(audioConfig, videoConfig)
        } catch (e: Exception) {
            error.postValue("Failed to create CaptureLiveStream: ${e.message ?: "Unknown error"}")
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface) {
        try {
            captureStream.startCapture(previewSurface)
        } catch (e: Exception) {
            error.postValue("startCapture: ${e.message ?: "Unknown error"}")
        }
    }

    fun stopCapture() {
        captureStream.stopCapture()
    }

    fun startStream() {
        try {
            if (captureStream is CaptureSrtLiveStream) {
                (captureStream as CaptureSrtLiveStream).connect(
                    configuration.endpoint.connection.ip,
                    configuration.endpoint.connection.port
                )
            } else if (captureStream is CaptureFileStream) {
                (captureStream as CaptureFileStream).file = File(
                    (getApplication() as Context).getExternalFilesDir(Environment.DIRECTORY_DCIM),
                    configuration.endpoint.file.filename
                )
            }
            captureStream.startStream()
        } catch (e: Exception) {
            error.postValue("startStream: ${e.message ?: "Unknown error"}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        captureStream.stopStream()
        if (captureStream is CaptureSrtLiveStream) {
            (captureStream as CaptureSrtLiveStream).disconnect()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        if (captureStream.videoSource.cameraId == "0") {
            captureStream.changeVideoSource("1")
        } else {
            captureStream.changeVideoSource("0")
        }
    }

    override fun onCleared() {
        super.onCleared()
        captureStream.release()
    }
}
