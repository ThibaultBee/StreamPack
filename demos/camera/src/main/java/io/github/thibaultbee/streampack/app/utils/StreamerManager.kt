/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.app.utils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.app.configuration.Configuration
import io.github.thibaultbee.streampack.ext.srt.data.SrtConnectionDescriptor
import io.github.thibaultbee.streampack.ext.srt.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.internal.endpoints.composites.IPublicCompositeEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import io.github.thibaultbee.streampack.ui.views.PreviewView
import io.github.thibaultbee.streampack.internal.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.utils.ChunkedFileOutputStream
import io.github.thibaultbee.streampack.utils.backCameraList
import io.github.thibaultbee.streampack.utils.frontCameraList
import io.github.thibaultbee.streampack.utils.getCameraStreamer
import io.github.thibaultbee.streampack.utils.getFileStreamer
import io.github.thibaultbee.streampack.utils.getLiveStreamer
import io.github.thibaultbee.streampack.utils.getStreamer
import io.github.thibaultbee.streampack.utils.isBackCamera
import kotlinx.coroutines.runBlocking
import java.io.File


class StreamerManager(
    private val context: Context,
    private val configuration: Configuration
) {
    private var streamer: IStreamer? = null

    var onErrorListener: OnErrorListener?
        get() = streamer?.onErrorListener
        set(value) {
            streamer?.onErrorListener = value
        }

    var onConnectionListener: OnConnectionListener?
        get() = streamer?.getLiveStreamer()?.onConnectionListener
        set(value) {
            streamer?.getLiveStreamer()?.onConnectionListener = value
        }

    val cameraId: String?
        get() = streamer?.getCameraStreamer()?.videoSource?.cameraId

    val streamerLifeCycleObserver: StreamerLifeCycleObserver by lazy {
        StreamerLifeCycleObserver(streamer!!)
    }

    private fun getSrtLiveStreamer(): ISrtLiveStreamer? {
        (streamer?.endpoint as IPublicCompositeEndpoint).muxer.info
        return streamer?.getStreamer<ISrtLiveStreamer>()
    }

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            streamer?.getFileStreamer()?.let {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            return permissions
        }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun rebuildStreamer() {
        streamer = StreamerFactory(context, configuration).build()
    }

    fun inflateStreamerView(view: PreviewView) {
        view.streamer = streamer?.getCameraStreamer()
    }

    suspend fun startStream() {
        if (streamer?.getLiveStreamer() != null) {
            getSrtLiveStreamer()?.let {
                val connection = SrtConnectionDescriptor(
                    configuration.endpoint.srt.ip,
                    configuration.endpoint.srt.port,
                    configuration.endpoint.srt.streamID,
                    configuration.endpoint.srt.passPhrase
                )
                it.connect(
                    connection
                )
            } ?: streamer?.getLiveStreamer()?.connect(
                configuration.endpoint.rtmp.url
            )
        }

        streamer?.getFileStreamer()?.let {
            /**
             * Use OutputStream.
             * FYI, outputStream is closed by stopStream.
             * To cut the video into multiple parts/chunks, use [ChunkedFileOutputStream].
             */
            it.outputStream =
                context.createVideoMediaOutputStream(configuration.endpoint.file.filename)
                    ?: throw Exception("Unable to create video output stream")
            /**
             *  Or use [File].
             *  It is not appropriate to directly access a [File]. Use Androidx FileProvider instead.
             */
//            it.file = File(
//                filesDir,
//                configuration.endpoint.file.filename
//            )
        }

        streamer?.startStream()
    }

    fun stopStream() {
        runBlocking {
            streamer?.stopStream()
        }
        streamer?.getLiveStreamer()?.disconnect()
    }

    fun release() {
        streamer?.release()
    }

    fun toggleCamera() {
        streamer?.getCameraStreamer()?.let {
            // Handle devices with only one camera
            val cameras = if (context.isBackCamera(it.camera)) {
                context.frontCameraList
            } else {
                context.backCameraList
            }
            if (cameras.isNotEmpty()) {
                it.camera = cameras[0]
            }
        }
    }

    val cameraSettings: CameraSettings?
        get() = streamer?.getCameraStreamer()?.videoSource?.settings


    var isMuted: Boolean
        get() = streamer?.audioSource?.isMuted ?: true
        set(value) {
            streamer?.audioSource?.isMuted = value
        }
}