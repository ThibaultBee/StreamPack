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
import android.net.Uri
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import io.github.thibaultbee.streampack.app.configuration.Configuration
import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.app.models.FileExtension
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.observers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.core.utils.extensions.backCameras
import io.github.thibaultbee.streampack.core.utils.extensions.frontCameras
import io.github.thibaultbee.streampack.core.utils.extensions.isBackCamera
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import io.github.thibaultbee.streampack.ext.srt.regulator.controllers.DefaultSrtBitrateRegulatorController
import io.github.thibaultbee.streampack.ui.views.PreviewView
import kotlinx.coroutines.flow.StateFlow


class StreamerManager(
    private val context: Context,
    private val configuration: Configuration
) {
    private val streamer: ICoroutineStreamer =
        DefaultCameraStreamer(context, configuration.audio.enable)

    val throwable: StateFlow<Throwable?> = streamer.throwable

    val isOpen: StateFlow<Boolean> = streamer.isOpen

    val isStreaming: StateFlow<Boolean> = streamer.isStreaming

    val cameraId: String?
        get() = (streamer as ICameraStreamer?)?.videoSource?.cameraId

    val streamerLifeCycleObserver: StreamerLifeCycleObserver by lazy {
        CustomStreamerLifeCycleObserver(streamer)
    }

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            // Only needed for File (MP4, TS, FLV,...)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return permissions
        }

    private val tsServiceInfo: TSServiceInfo
        get() = TSServiceInfo(
            TSServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

    private val bitrateRegulatorConfig: BitrateRegulatorConfig
        get() = BitrateRegulatorConfig(
            configuration.endpoint.srt.videoBitrateRange,
            Range(configuration.audio.bitrate, configuration.audio.bitrate)
        )

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configureStreamer() {
        val videoConfig = VideoConfig(
            mimeType = configuration.video.encoder,
            startBitrate = configuration.video.bitrate * 1000, // to b/s
            resolution = configuration.video.resolution,
            fps = configuration.video.fps,
            profile = configuration.video.profile,
            level = configuration.video.level
        )

        val audioConfig = AudioConfig(
            mimeType = configuration.audio.encoder,
            startBitrate = configuration.audio.bitrate,
            sampleRate = configuration.audio.sampleRate,
            channelConfig = AudioConfig.getChannelConfig(configuration.audio.numberOfChannels),
            profile = configuration.audio.profile,
            byteFormat = configuration.audio.byteFormat
        )
        if (configuration.video.enable) {
            streamer.configure(videoConfig)
        }

        if (configuration.audio.enable) {
            streamer.configure(audioConfig)
        }
    }

    suspend fun inflateStreamerView(view: PreviewView) {
        view.streamer = (streamer as ICameraStreamer?)
        view.startPreview()
    }

    suspend fun startStream() {
        val descriptor = when (configuration.endpoint.endpointType) {
            EndpointType.TS_FILE -> UriMediaDescriptor(
                context,
                context.createVideoContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.TS.extension
                    )
                )
            )

            EndpointType.FLV_FILE -> UriMediaDescriptor(
                context,
                context.createVideoContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.FLV.extension
                    )
                )
            )

            EndpointType.MP4_FILE -> UriMediaDescriptor(
                context,
                context.createVideoContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.MP4.extension
                    )
                )
            )

            EndpointType.WEBM_FILE -> UriMediaDescriptor(
                context,
                context.createVideoContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.WEBM.extension
                    )
                )
            )

            EndpointType.OGG_FILE -> UriMediaDescriptor(
                context,
                context.createAudioContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.OGG.extension
                    )
                )
            )

            EndpointType.THREEGP_FILE -> UriMediaDescriptor(
                context,
                context.createVideoContentUri(
                    configuration.endpoint.file.filename.appendIfNotEndsWith(
                        FileExtension.THREEGP.extension
                    )
                )
            )

            EndpointType.SRT -> SrtMediaDescriptor(
                configuration.endpoint.srt.ip,
                configuration.endpoint.srt.port,
                configuration.endpoint.srt.streamID,
                configuration.endpoint.srt.passPhrase,
                serviceInfo = tsServiceInfo
            )

            EndpointType.RTMP -> UriMediaDescriptor(Uri.parse(configuration.endpoint.rtmp.url))
        }

        if (configuration.endpoint.endpointType == EndpointType.SRT) {
            if (configuration.endpoint.srt.enableBitrateRegulation) {
                streamer.addBitrateRegulatorController(
                    DefaultSrtBitrateRegulatorController.Factory(
                        bitrateRegulatorConfig = bitrateRegulatorConfig
                    )
                )
            } else {
                streamer.removeBitrateRegulatorController()
            }
        }

        try {
            streamer.startStream(descriptor)
        } catch (t: Throwable) {
            streamer.close()
            throw t
        }
    }

    suspend fun stopStream() {
        streamer.stopStream()
        streamer.close()
    }

    fun release() {
        streamer.release()
    }

    fun toggleCamera() {
        (streamer as ICameraStreamer?)?.let {
            // Handle devices with only one camera
            val cameras = if (context.isBackCamera(it.camera)) {
                context.frontCameras
            } else {
                context.backCameras
            }
            if (cameras.isNotEmpty()) {
                it.camera = cameras[0]
            }
        }
    }

    val cameraSettings: CameraSettings?
        get() = (streamer as ICameraStreamer?)?.videoSource?.settings


    var isMuted: Boolean
        get() = streamer.audioSource?.isMuted ?: true
        set(value) {
            streamer.audioSource?.isMuted = value
        }

    class CustomStreamerLifeCycleObserver(streamer: ICoroutineStreamer) :
        StreamerLifeCycleObserver(streamer) {
        override fun onDestroy(owner: LifecycleOwner) {
            // Do nothing
            // The ViewModel onCleared() method will call release() method
        }
    }

    companion object {
        private const val TAG = "StreamerManager"
    }
}