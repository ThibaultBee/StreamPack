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
package io.github.thibaultbee.streampack.core.streamers.interfaces

import android.Manifest
import android.net.Uri
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.encoders.IEncoder
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.DefaultStreamer
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * A Streamer that is agnostic to the underlying implementation (either with coroutines or callbacks).
 */
interface IStreamer {
    /**
     * Audio configuration.
     *
     * @see [configure]
     */
    val audioConfig: AudioConfig?

    /**
     * Video configuration.
     *
     * @see [configure]
     */
    val videoConfig: VideoConfig?

    /**
     * Advanced settings for the audio source.
     */
    val audioSource: IAudioSource?

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IEncoder?

    /**
     * Advanced settings for the video source.
     */
    val videoSource: IVideoSource?

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IEncoder?

    /**
     * Advanced settings for the endpoint.
     */
    val endpoint: IEndpoint

    /**
     * Configuration information
     */
    val info: IConfigurationInfo

    /**
     * Gets configuration information
     */
    fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo

    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configure(audioConfig: AudioConfig)

    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     * @see [release]
     */
    fun configure(videoConfig: VideoConfig)

    /**
     * Configures both video and audio settings.
     * It is the first method to call after a [DefaultStreamer] instantiation.
     * It must be call when both stream and audio and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     * @see [release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configure(audioConfig: AudioConfig, videoConfig: VideoConfig) {
        configure(audioConfig)
        configure(videoConfig)
    }

    /**
     * Clean and reset the streamer.
     *
     * @see [configure]
     */
    fun release()

    /**
     * Adds a bitrate regulator controller to the streamer.
     */
    fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory)

    /**
     * Removes the bitrate regulator controller from the streamer.
     */
    fun removeBitrateRegulatorController()
}

/**
 * A Streamer based on coroutines.
 */
interface ICoroutineStreamer : IStreamer {
    /**
     * Returns the last throwable that occurred.
     */
    val throwable: StateFlow<Throwable?>

    /**
     * Returns true if endpoint is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpen: StateFlow<Boolean>

    /**
     * Returns true if stream is running.
     */
    val isStreaming: StateFlow<Boolean>

    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    suspend fun open(descriptor: MediaDescriptor)

    /**
     * Closes the streamer endpoint.
     */
    suspend fun close()

    /**
     * Starts audio/video stream.
     *
     * @see [stopStream]
     */
    suspend fun startStream()

    /**
     * Stops audio/video stream.
     *
     * @see [startStream]
     */
    suspend fun stopStream()
}

/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
suspend fun ICoroutineStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
suspend fun ICoroutineStreamer.open(uriString: String) =
    open(UriMediaDescriptor(Uri.parse(uriString)))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param descriptor The media descriptor to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(descriptor: MediaDescriptor) {
    open(descriptor)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(uri: Uri) {
    open(uri)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(uriString: String) {
    open(uriString)
    startStream()
}

interface ICallbackStreamer : IStreamer {
    /**
     * Returns true if endpoint is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpen: Boolean

    /**
     * Returns true if stream is running.
     */
    val isStreaming: Boolean

    /**
     * Opens the streamer endpoint asynchronously.
     *
     * The open error is returned through [Listener.onOpenFailed].
     * Also, you can use [Listener.onIsOpenChanged] to know when the endpoint is opened.
     *
     * @param descriptor Media descriptor to open
     */
    fun open(descriptor: MediaDescriptor)

    /**
     * Closes the streamer endpoint.
     */
    fun close()

    /**
     * Starts audio/video stream asynchronously.
     *
     * You must call [open] before calling this method.
     * The streamer must be opened before starting the stream. You can use [Listener.onIsOpenChanged].
     *
     * @see [stopStream]
     */
    fun startStream()

    /**
     * Starts audio/video stream asynchronously.
     *
     * Same as doing [open] and [startStream].
     *
     * @see [stopStream]
     */
    fun startStream(descriptor: MediaDescriptor)

    /**
     * Stops audio/video stream asynchronously.
     *
     * @see [startStream]
     */
    fun stopStream()

    /**
     * Adds a listener to the streamer.
     */
    fun addListener(listener: Listener)

    /**
     * Removes a listener from the streamer.
     */
    fun removeListener(listener: Listener)

    /**
     * Listener for the callback streamer.
     */
    interface Listener {
        /**
         * Called when the streamer opening failed.
         *
         * @param t The throwable that occurred
         */
        fun onOpenFailed(t: Throwable) = Unit

        /**
         * Called when the streamer is opened or closed.
         */
        fun onIsOpenChanged(isOpen: Boolean) = Unit

        /**
         * Called when the streamer was closed by an error.
         *
         * @param t The reason why the streamer was closed
         */
        fun onClose(t: Throwable) = Unit

        /**
         * Called when the stream is started or stopped.
         */
        fun onIsStreamingChanged(isStarted: Boolean) = Unit

        /**
         * Called when an error occurs.
         *
         * @param throwable The throwable that occurred
         */
        fun onError(throwable: Throwable) = Unit
    }
}


/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
fun ICallbackStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
fun ICallbackStreamer.open(uriString: String) =
    open(UriMediaDescriptor(Uri.parse(uriString)))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [stopStream]
 */
fun ICallbackStreamer.startStream(uri: Uri) = startStream(UriMediaDescriptor(uri))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [stopStream]
 */
fun ICallbackStreamer.startStream(uriString: String) = startStream(Uri.parse(uriString))
