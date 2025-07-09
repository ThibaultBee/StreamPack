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
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoRotation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * An output component for a streamer.
 */
interface IPipelineOutput : IStreamer {
    /**
     * Whether the output has audio.
     */
    val withAudio: Boolean

    /**
     * Whether the output has video.
     */
    val withVideo: Boolean
}

/**
 * Whether the output is streaming.
 */
val IPipelineOutput.isStreaming: Boolean
    get() = isStreamingFlow.value

/**
 * An internal output component for a pipeline.
 */
interface IPipelineEventOutputInternal : IPipelineOutput {
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
interface IConfigurableVideoPipelineOutputInternal : IConfigurableVideoPipelineOutput {
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
 * @param targetRotation The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...).
 * @param isEncoderInputSurface Whether the [Surface] is used as an input for a video encoder.
 */
data class SurfaceDescriptor(
    val surface: Surface,
    val resolution: Size,
    @RotationValue val targetRotation: Int = 0,
    val isEncoderInputSurface: Boolean = false
)

/**
 * An internal audio output component for a pipeline.
 */
sealed interface IVideoPipelineOutputInternal : IPipelineOutput {
    /**
     * The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    @RotationValue
    val targetRotation: Int
}

/**
 * An internal video output component for a pipeline.
 */
interface IVideoSurfacePipelineOutputInternal : IVideoPipelineOutputInternal, IWithVideoRotation {
    /**
     * The [Surface] flow to render video.
     * For surface mode video encoder.
     */
    val surfaceFlow: StateFlow<SurfaceDescriptor?>
}

/**
 * An internal video output component for a pipeline.
 * The pipeline is responsible for pulling video [Frame] from the user.
 */
interface IVideoCallbackPipelineOutputInternal : IVideoPipelineOutputInternal {
    /**
     * The video [Frame] listener.
     */
    var videoFrameRequestedListener: OnFrameRequestedListener?
}

/**
 * An internal video output component for a pipeline.
 * The provider is responsible for pushing video [RawFrame] to the pipeline.
 */
interface IVideoSyncPipelineOutputInternal : IVideoPipelineOutputInternal {
    /**
     * Queues a video [RawFrame] to be encoded.
     *
     * The [RawFrame.rawBuffer] is a duplicate of the original frame. If you need to modify the
     * frame, you should create a new [ByteBuffer] and copy the data from the original frame. Do not
     * modify the original [RawFrame.rawBuffer].
     *
     * You should call [RawFrame.close] when you are done with the frame to release resources.
     *
     * Also, to avoid blocking other outputs, you should execute this method in another thread than
     * the calling thread.
     *
     * @param frame The video [RawFrame] to queue.
     */
    fun queueVideoFrame(frame: RawFrame)
}

/**
 * An internal audio output component for a pipeline.
 */
sealed interface IAudioPipelineOutputInternal : IPipelineOutput

/**
 * An internal audio output component for a pipeline.
 * The provider is responsible for pushing audio [RawFrame] to the pipeline.
 */
interface IAudioSyncPipelineOutputInternal : IAudioPipelineOutputInternal {
    /**
     * Queues an audio [RawFrame] to be encoded.
     *
     * The [RawFrame.rawBuffer] is a duplicate of the original frame. If you need to modify the
     * frame, you should create a new [ByteBuffer] and copy the data from the original frame. Do not
     * modify the original [RawFrame.rawBuffer].
     *
     * You must call [RawFrame.close] when you are done with the frame to release resources.
     *
     * Also, to avoid blocking other outputs, you should execute this method in another thread than
     * the calling thread.
     *
     * @param frame The audio [RawFrame] to queue.
     */
    fun queueAudioFrame(frame: RawFrame)
}

/**
 * An internal audio output component for a pipeline.
 * The pipeline is responsible for pulling audio [Frame] from the user.
 */
interface IAudioCallbackPipelineOutputInternal : IAudioPipelineOutputInternal {
    /**
     * The audio [Frame] listener.
     */
    var audioFrameRequestedListener: OnFrameRequestedListener?
}



