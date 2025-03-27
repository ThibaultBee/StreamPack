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
package io.github.thibaultbee.streampack.core.streamers.single

import android.net.Uri
import android.view.Surface
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotationToDegrees
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.IAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
import androidx.core.net.toUri

/**
 * The single streamer audio configuration.
 */
typealias AudioConfig = AudioCodecConfig

/**
 * The single streamer video configuration.
 */
typealias VideoConfig = VideoCodecConfig

/**
 * A single Streamer that is agnostic to the underlying implementation (either with coroutines or callbacks).
 */
interface ISingleStreamer {
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
     * Adds a bitrate regulator controller to the streamer.
     */
    fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory)

    /**
     * Removes the bitrate regulator controller from the streamer.
     */
    fun removeBitrateRegulatorController()
}

/**
 * An audio single Streamer
 */
interface IAudioSingleStreamer : IAudioStreamer {
    /**
     * Gets the audio configuration.
     */
    val audioConfig: AudioConfig?

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IEncoder?
}

/**
 * A video single streamer.
 */
interface IVideoSingleStreamer : IVideoStreamer {
    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    suspend fun setTargetRotation(@RotationValue rotation: Int)

    /**
     * Gets the video configuration.
     */
    val videoConfig: VideoConfig?

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IEncoder?
}

interface ICoroutineAudioSingleStreamer : ICoroutineAudioStreamer<AudioConfig>, IAudioSingleStreamer

interface ICoroutineVideoSingleStreamer : ICoroutineVideoStreamer<VideoConfig>, IVideoSingleStreamer

/**
 * A single Streamer based on coroutines.
 */
interface ICoroutineSingleStreamer : ICoroutineStreamer, ISingleStreamer {
    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    suspend fun open(descriptor: MediaDescriptor)
}

/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
suspend fun ICoroutineSingleStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
suspend fun ICoroutineSingleStreamer.open(uriString: String) =
    open(UriMediaDescriptor(uriString.toUri()))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param descriptor The media descriptor to open
 * @see [ICoroutineSingleStreamer.stopStream]
 */
suspend fun ICoroutineSingleStreamer.startStream(descriptor: MediaDescriptor) {
    open(descriptor)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [ICoroutineSingleStreamer.stopStream]
 */
suspend fun ICoroutineSingleStreamer.startStream(uri: Uri) {
    open(uri)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [ICoroutineSingleStreamer.stopStream]
 */
suspend fun ICoroutineSingleStreamer.startStream(uriString: String) {
    open(uriString)
    startStream()
}
