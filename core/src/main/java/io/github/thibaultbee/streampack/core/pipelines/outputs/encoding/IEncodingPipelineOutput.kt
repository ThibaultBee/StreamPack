/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.pipelines.outputs.encoding

import android.net.Uri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutputInternal
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.single.open
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import kotlinx.coroutines.flow.StateFlow

/**
 * An output component for a streamer.
 */
interface IEncodingPipelineOutput : IPipelineOutput {
    /**
     * Advanced settings for the endpoint.
     */
    val endpoint: IEndpoint

    /**
     * Returns true if output is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpenFlow: StateFlow<Boolean>

    /**
     * Opens the streamer output.
     *
     * @param descriptor Media descriptor to open
     */
    suspend fun open(descriptor: MediaDescriptor)

    /**
     * Closes the streamer output.
     */
    suspend fun close()

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
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
suspend fun IEncodingPipelineOutput.open(uri: Uri) = open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
suspend fun IEncodingPipelineOutput.open(uriString: String) =
    open(UriMediaDescriptor(Uri.parse(uriString)))


/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param descriptor The media descriptor to open
 * @see [IEncodingPipelineOutput.stopStream]
 */
suspend fun IEncodingPipelineOutput.startStream(descriptor: MediaDescriptor) {
    open(descriptor)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [IEncodingPipelineOutput.stopStream]
 */
suspend fun IEncodingPipelineOutput.startStream(uri: Uri) {
    open(uri)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [IEncodingPipelineOutput.stopStream]
 */
suspend fun IEncodingPipelineOutput.startStream(uriString: String) {
    open(uriString)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}

/**
 * An audio encoding output component for a pipeline.
 */
interface IConfigurableAudioPipelineOutput {
    /**
     * The audio configuration flow.
     */
    val audioCodecConfigFlow: StateFlow<AudioCodecConfig?>

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IEncoder?

    /**
     * Configures only audio codec settings.
     *
     * @param audioCodecConfig The audio codec configuration
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig)
}

/**
 * An video encoding output component for a pipeline.
 */
interface IConfigurableVideoPipelineOutput {
    /**
     * The video configuration flow.
     */
    val videoCodecConfigFlow: StateFlow<VideoCodecConfig?>

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IEncoder?

    /**
     * Configures only video codec settings.
     *
     * @param videoCodecConfig The video codec configuration
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig)
}

interface IConfigurableEncodingPipelineOutput : IEncodingPipelineOutput,
    IConfigurableAudioPipelineOutput, IConfigurableVideoPipelineOutput

/**
 * An internal output component for a streamer.
 */
internal interface IConfigurableAudioPipelineOutputInternal : IConfigurableAudioPipelineOutput {
    /**
     * Audio configuration listener.
     */
    var audioConfigEventListener: Listener?

    /**
     * Audio configuration listener interface.
     */
    interface Listener {
        /**
         * It is called when audio configuration is set.
         * The listener can reject the configuration by throwing an exception.
         * It is used to validate and apply audio configuration to the source.
         */
        suspend fun onSetAudioCodecConfig(newAudioCodecConfig: AudioCodecConfig)
    }
}

/**
 * An internal output component for a streamer.
 */
internal interface IConfigurableVideoPipelineOutputInternal : IConfigurableVideoPipelineOutput {
    /**
     * Video configuration listener.
     */
    var videoConfigEventListener: Listener?

    /**
     * Video configuration listener interface.
     */
    interface Listener {
        /**
         * It is called when video configuration is set.
         * The listener can reject the configuration by throwing an exception.
         * It is used to validate and apply video configuration to the source.
         */
        suspend fun onSetVideoCodecConfig(newVideoCodecConfig: VideoCodecConfig)
    }
}

internal interface IEncodingPipelineOutputInternal : IConfigurableEncodingPipelineOutput,
    IConfigurableAudioPipelineOutputInternal,
    IConfigurableVideoPipelineOutputInternal, IPipelineOutputInternal