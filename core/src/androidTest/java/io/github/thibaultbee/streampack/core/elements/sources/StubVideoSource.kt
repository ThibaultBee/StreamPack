package io.github.thibaultbee.streampack.core.elements.sources

import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoFrameSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class StubVideoSurfaceSource(override val timestampOffsetInNs: Long = 0) : StubVideoSource(),
    ISurfaceSourceInternal {
    private var outputSurface: Surface? = null
    override suspend fun getOutput() = outputSurface

    override suspend fun setOutput(surface: Surface) {
        outputSurface = surface
    }

    override suspend fun resetOutput() {
        outputSurface = null
    }
}

class StubVideoFrameSource : StubVideoSource(), IVideoFrameSourceInternal {
    override fun getVideoFrame(buffer: ByteBuffer): RawFrame {
        return RawFrame(
            buffer,
            0L
        )
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

    override fun configure(config: VideoSourceConfig) {
        _configurationFlow.value = config
    }

    override fun release() {

    }
}