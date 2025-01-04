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
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.utils.SurfaceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class StubAudioAsyncPipelineOutput :
    StubPipelineOutput(hasAudio = true, hasVideo = false),
    IAudioSyncPipelineOutputInternal {

    private val _audioFrameFlow = MutableStateFlow<Frame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: Frame) {
        _audioFrameFlow.value = frame
    }
}

open class StubVideoSurfacePipelineOutput(resolution: Size) :
    StubPipelineOutput(hasAudio = false, hasVideo = true),
    IVideoSurfacePipelineOutputInternal {

    override var targetRotation: Int = 0
    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
    override var videoSourceTimestampOffset: Long = 0L
}

class StubAudioSyncVideoSurfacePipelineOutput(resolution: Size) :
    StubPipelineOutput(hasAudio = true, hasVideo = true),
    IAudioSyncPipelineOutputInternal, IVideoSurfacePipelineOutputInternal {

    override var targetRotation: Int = 0
    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
    override var videoSourceTimestampOffset: Long = 0L

    private val _audioFrameFlow = MutableStateFlow<Frame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: Frame) {
        _audioFrameFlow.value = frame
    }
}

abstract class StubPipelineOutput(override val hasAudio: Boolean, override val hasVideo: Boolean) :
    IPipelineOutput {

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow = _throwableFlow.asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    override suspend fun startStream() {
        if (isStreamingFlow.value) {
            Logger.w(TAG, "Stream is already running")
            return
        }
        Logger.i(TAG, "Start stream called")
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        if (!isStreamingFlow.value) {
            Logger.w(TAG, "Stream is not running")
            return
        }
        Logger.i(TAG, "Stop stream called")
        _isStreamingFlow.emit(false)
    }

    override suspend fun release() {
        Logger.i(TAG, "Release called")
    }

    companion object {
        private const val TAG = "DummyPipelineOutput"
    }
}

class StubAudioSyncVideoSurfacePipelineOutputInternal(resolution: Size) :
    StubPipelineOutputInternal(hasAudio = true, hasVideo = true),
    IAudioSyncPipelineOutputInternal, IVideoSurfacePipelineOutputInternal {

    override var targetRotation: Int = 0
    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
    override var videoSourceTimestampOffset: Long = 0L

    private val _audioFrameFlow = MutableStateFlow<Frame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: Frame) {
        _audioFrameFlow.value = frame
    }
}

class StubVideoSurfacePipelineOutputInternal(resolution: Size) :
    StubPipelineOutputInternal(hasAudio = false, hasVideo = true),
    IVideoSurfacePipelineOutputInternal {

    override var targetRotation: Int = 0
    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
    override var videoSourceTimestampOffset: Long = 0L
}

abstract class StubPipelineOutputInternal(hasAudio: Boolean, hasVideo: Boolean) :
    StubPipelineOutput(hasAudio, hasVideo),
    IPipelineOutputInternal {

    override var streamEventListener: IPipelineOutputInternal.Listener? = null

    override suspend fun startStream() {
        streamEventListener?.onStartStream()
        super.startStream()
    }

    override suspend fun stopStream() {
        streamEventListener?.onStopStream()
        super.stopStream()
    }
}

internal class StubAudioSyncConfigurableEncodingPipelineOutputInternal :
    StubAudioAsyncPipelineOutput(),
    IConfigurableAudioPipelineOutputInternal {
    override var audioConfigEventListener: IConfigurableAudioPipelineOutputInternal.Listener? = null

    private val _audioCodecConfigFlow = MutableStateFlow<AudioCodecConfig?>(null)
    override val audioCodecConfigFlow = _audioCodecConfigFlow.asStateFlow()

    override val audioEncoder: IEncoder? = null

    override suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig) {
        audioConfigEventListener?.onSetAudioCodecConfig(audioCodecConfig)
        _audioCodecConfigFlow.emit(audioCodecConfig)
    }

}

internal class StubVideoSurfaceConfigurableEncodingPipelineOutputInternal :
    StubVideoSurfacePipelineOutput(resolution = Size(1280, 720)),
    IConfigurableVideoPipelineOutputInternal {

    override var videoConfigEventListener: IConfigurableVideoPipelineOutputInternal.Listener? = null

    private val _videoCodecConfigFlow = MutableStateFlow<VideoCodecConfig?>(null)
    override val videoCodecConfigFlow = _videoCodecConfigFlow.asStateFlow()

    override val videoEncoder: IEncoder? = null

    override suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig) {
        videoConfigEventListener?.onSetVideoCodecConfig(videoCodecConfig)
        _videoCodecConfigFlow.emit(videoCodecConfig)
    }
}