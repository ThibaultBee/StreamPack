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
package io.github.thibaultbee.streampack.core.streamers.single

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.pipelines.inputs.IAudioInput
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo

/**
 * Creates a [AudioOnlySingleStreamer] with a default audio source.
 *
 * @param context the application context
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If parameter is null, no audio source are set. It can be set later with [AudioOnlySingleStreamer.setAudioSource].
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 */
suspend fun AudioOnlySingleStreamer(
    context: Context,
    audioSourceFactory: IAudioSourceInternal.Factory = MicrophoneSourceFactory(),
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory()
): AudioOnlySingleStreamer {
    val streamer = AudioOnlySingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
    )
    streamer.setAudioSource(audioSourceFactory)
    return streamer
}

/**
 * A [ISingleStreamer] implementation for audio only (without video).
 *
 * @param context the application context
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 */
class AudioOnlySingleStreamer(
    context: Context,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory()
) : ISingleStreamer, IAudioSingleStreamer {
    private val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        withAudio = true,
        withVideo = false
    )
    override val throwableFlow = streamer.throwableFlow
    override val isOpenFlow = streamer.isOpenFlow
    override val isStreamingFlow = streamer.isStreamingFlow
    override val endpoint: IEndpoint
        get() = streamer.endpoint
    override val info: IConfigurationInfo
        get() = streamer.info

    override val audioConfigFlow = streamer.audioConfigFlow
    override val audioInput: IAudioInput = streamer.audioInput!!
    
    override val audioEncoder: IEncoder?
        get() = streamer.audioEncoder

    override fun getInfo(descriptor: MediaDescriptor) = streamer.getInfo(descriptor)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: AudioConfig) =
        streamer.setAudioConfig(audioConfig)

    override suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) =
        streamer.setAudioSource(audioSourceFactory)

    override suspend fun open(descriptor: MediaDescriptor) = streamer.open(descriptor)

    override suspend fun close() = streamer.close()

    override suspend fun startStream() = streamer.startStream()

    override suspend fun stopStream() = streamer.stopStream()

    override suspend fun release() = streamer.release()

    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) {
        throw UnsupportedOperationException("Audio single streamer does not support bitrate regulator controller")
    }

    override fun removeBitrateRegulatorController() {
        // Do nothing
    }
}