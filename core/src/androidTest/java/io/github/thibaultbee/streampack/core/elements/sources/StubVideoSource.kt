package io.github.thibaultbee.streampack.core.elements.sources

import android.media.MediaFormat
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoFrameSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class StubVideoSurfaceSource(override val timestampOffset: Long = 0) : StubVideoSource(),
    ISurfaceSource {
    override var outputSurface: Surface? = null
}

class StubVideoFrameSource : StubVideoSource(), IVideoFrameSource {
    override fun getVideoFrame(buffer: ByteBuffer): Frame {
        return Frame(
            buffer,
            0L,
            format = MediaFormat().apply {
                setString(
                    MediaFormat.KEY_MIME,
                    MediaFormat.MIMETYPE_VIDEO_RAW
                )
            })
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