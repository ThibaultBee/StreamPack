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
package com.github.thibaultbee.streampack.app.utils

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.internal.utils.AudioMeasurements
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import com.github.thibaultbee.streampack.utils.CameraSettings
import com.github.thibaultbee.streampack.utils.getBackCameraList
import com.github.thibaultbee.streampack.utils.getFrontCameraList
import com.github.thibaultbee.streampack.utils.isBackCamera
import java.io.File

class StreamerManager(
    private val context: Context,
    private val configuration: Configuration
) {
    private lateinit var streamer: IStreamer

    var onErrorListener: OnErrorListener?
        get() = streamer.onErrorListener
        set(value) {
            streamer.onErrorListener = value
        }

    var onConnectionListener: OnConnectionListener?
        get() = getLiveStreamer()?.onConnectionListener
        set(value) {
            getLiveStreamer()?.onConnectionListener = value
        }

    val audioMeasurements: AudioMeasurements?
        get() = getBaseStreamer()?.measurements?.audio

    val cameraId: String?
        get() {
            return if (streamer is ICameraStreamer) {
                val cameraStreamer = streamer as ICameraStreamer
                cameraStreamer.camera
            } else {
                null
            }
        }

    private inline fun <reified T> getStreamer(): T? {
        return if (streamer is T) {
            streamer as T
        } else {
            null
        }
    }

    private fun getCameraStreamer(): ICameraStreamer? {
        return getStreamer<ICameraStreamer>()
    }

    private fun getLiveStreamer(): ILiveStreamer? {
        return getStreamer<ILiveStreamer>()
    }

    private fun getFileStreamer(): IFileStreamer? {
        return getStreamer<IFileStreamer>()
    }

    private fun getBaseStreamer(): IStreamer? {
        return getStreamer<IStreamer>()
    }

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            getFileStreamer()?.let {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return permissions
        }

    fun rebuildStreamer() {
        streamer = StreamerFactory(context, configuration).build()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface) {
        getCameraStreamer()?.startPreview(previewSurface)
    }

    fun stopPreview() {
        getCameraStreamer()?.stopPreview()
    }

    suspend fun startStream(filesDir: File) {
        getLiveStreamer()?.let {
            it.streamId =
                configuration.endpoint.connection.streamID
            it.passPhrase =
                configuration.endpoint.connection.passPhrase
            it.connect(
                configuration.endpoint.connection.ip,
                configuration.endpoint.connection.port
            )
        }
        getFileStreamer()?.let {
            it.file = File(
                filesDir,
                configuration.endpoint.file.filename
            )
        }
        streamer.startStream()
    }

    fun stopStream() {
        streamer.stopStream()
        getLiveStreamer()?.disconnect()
    }

    fun release() {
        streamer.release()
    }

    fun toggleCamera() {
        getCameraStreamer()?.let {
            if (context.isBackCamera(it.camera)) {
                it.camera = context.getFrontCameraList()[0]
            } else {
                it.camera = context.getBackCameraList()[0]
            }
        }
    }

    val cameraSettings: CameraSettings?
        get() = getCameraStreamer()?.cameraSettings
}