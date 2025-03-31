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
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.sourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.mapState
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.utils.SurfaceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class StubAudioAsyncPipelineOutput :
    StubPipelineOutput(withAudio = true, withVideo = false),
    IAudioSyncPipelineOutputInternal {

    private val _audioFrameFlow = MutableStateFlow<RawFrame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: RawFrame) {
        _audioFrameFlow.value = frame
    }
}

open class StubVideoSurfacePipelineOutput(resolution: Size) :
    StubPipelineOutput(withAudio = false, withVideo = true),
    IVideoSurfacePipelineOutputInternal {

    override suspend fun setTargetRotation(@RotationValue rotation: Int) = Unit

    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
}

class StubAudioSyncVideoSurfacePipelineOutput(resolution: Size) :
    StubPipelineOutput(withAudio = true, withVideo = true),
    IAudioSyncPipelineOutputInternal, IVideoSurfacePipelineOutputInternal {

    override suspend fun setTargetRotation(@RotationValue rotation: Int) = Unit

    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()

    private val _audioFrameFlow = MutableStateFlow<RawFrame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: RawFrame) {
        _audioFrameFlow.value = frame
    }
}

abstract class StubPipelineOutput(
    override val withAudio: Boolean,
    override val withVideo: Boolean
) :
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

    override suspend fun setTargetRotation(@RotationValue rotation: Int) = Unit

    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()

    private val _audioFrameFlow = MutableStateFlow<RawFrame?>(null)
    val audioFrameFlow = _audioFrameFlow.asStateFlow()

    override fun queueAudioFrame(frame: RawFrame) {
        _audioFrameFlow.value = frame
    }
}

class StubVideoSurfacePipelineOutputInternal(resolution: Size) :
    StubPipelineOutputInternal(hasAudio = false, hasVideo = true),
    IVideoSurfacePipelineOutputInternal {

    override suspend fun setTargetRotation(@RotationValue rotation: Int) = Unit
    
    private val _surfaceFlow =
        MutableStateFlow<SurfaceWithSize?>(
            SurfaceWithSize(
                SurfaceUtils.createSurface(resolution),
                resolution
            )
        )
    override val surfaceFlow = _surfaceFlow.asStateFlow()
}

abstract class StubPipelineOutputInternal(hasAudio: Boolean, hasVideo: Boolean) :
    StubPipelineOutput(hasAudio, hasVideo),
    IPipelineEventOutputInternal {

    override var streamEventListener: IPipelineEventOutputInternal.Listener? = null

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
    IConfigurableAudioEncodingPipelineOutput,
    IConfigurableAudioPipelineOutputInternal {
    override var audioConfigEventListener: IConfigurableAudioPipelineOutputInternal.Listener? = null

    private val _audioCodecConfigFlow = MutableStateFlow<AudioCodecConfig?>(null)
    override val audioCodecConfigFlow = _audioCodecConfigFlow.asStateFlow()
    override val audioSourceConfigFlow = audioCodecConfigFlow.mapState { it?.sourceConfig }

    override val audioEncoder: IEncoder? = null

    override suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig) {
        audioConfigEventListener?.onSetAudioSourceConfig(audioCodecConfig.sourceConfig)
        _audioCodecConfigFlow.emit(audioCodecConfig)
    }
}

internal class StubVideoSurfaceConfigurableEncodingPipelineOutputInternal :
    StubVideoSurfacePipelineOutput(resolution = Size(1280, 720)),
    IConfigurableVideoEncodingPipelineOutput,
    IConfigurableVideoPipelineOutputInternal {

    override var videoConfigEventListener: IConfigurableVideoPipelineOutputInternal.Listener? = null

    private val _videoCodecConfigFlow = MutableStateFlow<VideoCodecConfig?>(null)
    override val videoCodecConfigFlow = _videoCodecConfigFlow.asStateFlow()
    override val videoSourceConfigFlow: StateFlow<VideoSourceConfig?> =
        videoCodecConfigFlow.mapState { it?.sourceConfig }

    override val videoEncoder: IEncoder? = null

    override suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig) {
        videoConfigEventListener?.onSetVideoSourceConfig(videoCodecConfig.sourceConfig)
        _videoCodecConfigFlow.emit(videoCodecConfig)
    }
}