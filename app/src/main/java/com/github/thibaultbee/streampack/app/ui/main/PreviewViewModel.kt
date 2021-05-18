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
import android.os.Environment
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.app.configuration.Configuration.Endpoint.EndpointType
import com.github.thibaultbee.streampack.app.utils.StreamPackLogger
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.streamer.BaseCaptureStreamer
import com.github.thibaultbee.streampack.streamer.CaptureFileStreamer
import com.github.thibaultbee.streampack.streamer.CaptureSrtLiveStreamer
import java.io.File

class PreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = this::class.java.simpleName

    private val logger = StreamPackLogger()

    private val configuration = Configuration(getApplication())

    private lateinit var captureStreamer: BaseCaptureStreamer

    val cameraId: String
        get() = captureStreamer.camera

    val error = MutableLiveData<String>()

    val streamAdditionalPermissions: List<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (captureStreamer is CaptureFileStreamer) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return permissions
        }

    fun createStreamer() {
        val tsServiceInfo = ServiceInfo(
            ServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

        try {
            captureStreamer = if (configuration.endpoint.enpointType == EndpointType.SRT) {
                CaptureSrtLiveStreamer(getApplication(), tsServiceInfo, logger)
            } else {
                CaptureFileStreamer(getApplication(), tsServiceInfo, logger)
            }

            captureStreamer.onErrorListener = object : OnErrorListener {
                override fun onError(source: String, message: String) {
                    error.postValue("$source: $message")
                }
            }

            if (captureStreamer is CaptureSrtLiveStreamer) {
                (captureStreamer as CaptureSrtLiveStreamer).onConnectionListener =
                    object : OnConnectionListener {
                        override fun onLost(message: String) {
                            error.postValue("Connection lost: $message")
                        }

                        override fun onFailed(message: String) {
                            error.postValue("Connection failed: $message")
                        }

                        override fun onSuccess() {
                            Log.i(TAG, "Connection succeeded")
                        }
                    }
            }
            Log.d(TAG, "Streamer is created")
        } catch (e: Exception) {
            Log.e(TAG, "createStreamer failed", e)
            error.postValue("createStreamer: ${e.message ?: "Unknown error"}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun configureStreamer() {
        val videoConfig =
            VideoConfig(
                mimeType = configuration.video.encoder,
                startBitrate = configuration.video.bitrate * 1000, // to b/s
                resolution = configuration.video.resolution,
                fps = configuration.video.fps
            )

        val audioConfig = AudioConfig(
            mimeType = configuration.audio.encoder,
            startBitrate = configuration.audio.bitrate,
            sampleRate = configuration.audio.sampleRate,
            channelConfig = configuration.audio.channelConfiguration,
            byteFormat = configuration.audio.byteFormat
        )

        try {
            captureStreamer.configure(audioConfig, videoConfig)
            Log.d(TAG, "Streamer is configured")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure streamer", e)
            error.postValue("Failed to create CaptureLiveStream: ${e.message ?: "Unknown error"}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface) {
        try {
            captureStreamer.startPreview(previewSurface)
        } catch (e: Exception) {
            Log.e(TAG, "startPreview failed", e)
            error.postValue("startPreview: ${e.message ?: "Unknown error"}")
        }
    }

    fun stopPreview() {
        captureStreamer.stopPreview()
    }

    fun startStream() {
        try {
            if (captureStreamer is CaptureSrtLiveStreamer) {
                (captureStreamer as CaptureSrtLiveStreamer).connect(
                    configuration.endpoint.connection.ip,
                    configuration.endpoint.connection.port
                )
            } else if (captureStreamer is CaptureFileStreamer) {
                (captureStreamer as CaptureFileStreamer).file = File(
                    (getApplication() as Context).getExternalFilesDir(Environment.DIRECTORY_DCIM),
                    configuration.endpoint.file.filename
                )
            }
            captureStreamer.startStream()
        } catch (e: Exception) {
            Log.e(TAG, "startStream failed", e)
            error.postValue("startStream: ${e.message ?: "Unknown error"}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        captureStreamer.stopStream()
        if (captureStreamer is CaptureSrtLiveStreamer) {
            (captureStreamer as CaptureSrtLiveStreamer).disconnect()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        if (captureStreamer.camera == "0") {
            captureStreamer.camera = "1"
        } else {
            captureStreamer.camera = "0"
        }
    }

    override fun onCleared() {
        super.onCleared()
        captureStreamer.release()
    }
}
