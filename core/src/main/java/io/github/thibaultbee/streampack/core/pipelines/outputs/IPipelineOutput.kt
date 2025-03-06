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
package io.github.thibaultbee.streampack.core.pipelines.outputs

import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * An output component for a streamer.
 */
interface IPipelineOutput {
    /**
     * Whether the output has audio.
     */
    val hasAudio: Boolean

    /**
     * Whether the output has video.
     */
    val hasVideo: Boolean

    /**
     * Returns the last throwable that occurred.
     */
    val throwableFlow: StateFlow<Throwable?>

    /**
     * Returns true if output is running.
     */
    val isStreamingFlow: StateFlow<Boolean>

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

    /**
     * Clean and reset the output.
     */
    suspend fun release()
}

/**
 * Whether the output is streaming.
 */
val IPipelineOutput.isStreaming: Boolean
    get() = isStreamingFlow.value

/**
 * An internal output component for a pipeline.
 */
interface IPipelineOutputInternal : IPipelineOutput {
    /**
     * A listener for audio/video stream events.
     */
    var streamEventListener: Listener?

    /**
     * Audio/video stream listener interface.
     */
    interface Listener {
        /**
         * Called synchronously when the stream has started.
         * It is called on the same thread as [startStream].
         * The listener can throw an exception to prevent the stream from starting.
         */
        suspend fun onStartStream() {}

        /**
         * Called synchronously when the stream has stopped.
         * It is called on the same thread as [stopStream].
         */
        suspend fun onStopStream() {}
    }
}

/**
 * Clean and reset the output synchronously.
 *
 * @see [IPipelineOutput.release]
 */
fun IPipelineOutput.releaseBlocking() = runBlocking {
    release()
}

interface IConfigurableAudioPipelineOutput {
    /**
     * The audio configuration flow.
     */
    val audioSourceConfigFlow: StateFlow<AudioSourceConfig?>
}

/**
 * A configurable audio internal output component for a pipeline.
 */
interface IConfigurableAudioPipelineOutputInternal : IConfigurableAudioPipelineOutput {
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
        suspend fun onSetAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig)
    }
}

/**
 * A configurable video output component for a pipeline.
 */
interface IConfigurableVideoPipelineOutput {
    /**
     * The video configuration flow.
     */
    val videoSourceConfigFlow: StateFlow<VideoSourceConfig?>
}

/**
 * A configurable video internal output component for a pipeline.
 */
interface IConfigurableVideoPipelineOutputInternal: IConfigurableVideoPipelineOutput {
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
        suspend fun onSetVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig)
    }
}

// Pipeline outputs inputs interfaces

/**
 * A [Surface] with its resolution.
 *
 * @param surface The [Surface].
 * @param resolution The resolution of the [Surface].
 */
data class SurfaceWithSize(
    val surface: Surface, val resolution: Size
)

/**
 * An internal audio output component for a pipeline.
 */
sealed interface IVideoPipelineOutputInternal : IPipelineOutput

/**
 * An internal video output component for a pipeline.
 */
interface IVideoSurfacePipelineOutputInternal : IVideoPipelineOutputInternal {
    /**
     * The rotation in one the [Surface] rotations from the device natural orientation.
     */
    @RotationValue
    var targetRotation: Int

    /**
     * The [Surface] flow to render video.
     * For surface mode video encoder.
     */
    val surfaceFlow: StateFlow<SurfaceWithSize?>
}

/**
 * An internal video output component for a pipeline.
 * The pipeline is responsible for pulling video [Frame] from the user.
 */
interface IVideoAsyncPipelineOutputInternal : IVideoPipelineOutputInternal {
    /**
     * The video [Frame] listener.
     */
    var videoFrameRequestedListener: OnFrameRequestedListener?
}

/**
 * An internal video output component for a pipeline.
 * The provider is responsible for pushing video [Frame] to the pipeline.
 */
interface IVideoSyncPipelineOutputInternal : IVideoPipelineOutputInternal {
    /**
     * Queue an video [Frame] to be encoded.
     *
     * @param frame The video [Frame] to queue.
     */
    fun queueVideoFrame(frame: RawFrame)
}

/**
 * An internal audio output component for a pipeline.
 */
sealed interface IAudioPipelineOutputInternal : IPipelineOutput

/**
 * An internal audio output component for a pipeline.
 * The provider is responsible for pushing audio [Frame] to the pipeline.
 */
interface IAudioSyncPipelineOutputInternal : IAudioPipelineOutputInternal {
    /**
     * Queue an audio [Frame] to be encoded.
     *
     * @param frame The audio [Frame] to queue.
     */
    fun queueAudioFrame(frame: RawFrame)
}

/**
 * An internal audio output component for a pipeline.
 * The pipeline is responsible for pulling audio [Frame] from the user.
 */
interface IAudioAsyncPipelineOutputInternal : IAudioPipelineOutputInternal {
    /**
     * The audio [Frame] listener.
     */
    var audioFrameRequestedListener: OnFrameRequestedListener?
}



