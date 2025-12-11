package io.github.thibaultbee.streampack.core.elements.sources

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoFrameSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.pool.IReadOnlyRawFrameFactory
import io.github.thibaultbee.streampack.core.elements.utils.time.Timebase
import io.github.thibaultbee.streampack.core.pipelines.IVideoDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StubVideoSurfaceSource(override val timebase: Timebase = Timebase.REALTIME) :
    StubVideoSource(),
    ISurfaceSourceInternal {
    private var outputSurface: Surface? = null
    override suspend fun getOutput() = outputSurface

    override suspend fun setOutput(surface: Surface) {
        outputSurface = surface
    }

    override suspend fun resetOutput() {
        outputSurface = null
    }

    class Factory(val timebase: Timebase = Timebase.REALTIME) : IVideoSourceInternal.Factory {
        override suspend fun create(
            context: Context,
            dispatcherProvider: IVideoDispatcherProvider
        ): IVideoSourceInternal {
            return StubVideoSurfaceSource(timebase)
        }

        override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
            return source is StubVideoSurfaceSource
        }
    }
}

class StubVideoFrameSource : StubVideoSource(), IVideoFrameSourceInternal {
    override fun getVideoFrame(frameFactory: IReadOnlyRawFrameFactory) =
        frameFactory.create(8192, 0L)

    class Factory : IVideoSourceInternal.Factory {
        override suspend fun create(
            context: Context,
            dispatcherProvider: IVideoDispatcherProvider
        ): IVideoSourceInternal {
            return StubVideoFrameSource()
        }

        override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
            return source is StubVideoFrameSource
        }
    }
}

abstract class StubVideoSource : IVideoSourceInternal {
    override val infoProviderFlow: StateFlow<ISourceInfoProvider> =
        MutableStateFlow(DefaultSourceInfoProvider())

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _configurationFlow = MutableStateFlow<VideoSourceConfig?>(null)
    val configurationFlow = _configurationFlow.asStateFlow()

    override suspend fun startStream() {
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        _isStreamingFlow.emit(false)
    }

    override suspend fun configure(config: VideoSourceConfig) {
        _configurationFlow.value = config
    }

    override suspend fun release() = Unit
}