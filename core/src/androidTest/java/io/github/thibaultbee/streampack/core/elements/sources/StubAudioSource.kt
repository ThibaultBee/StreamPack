package io.github.thibaultbee.streampack.core.elements.sources

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.pool.IReadOnlyRawFrameFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StubAudioSource : IAudioSourceInternal {
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _configurationFlow = MutableStateFlow<AudioSourceConfig?>(null)
    val configurationFlow = _configurationFlow.asStateFlow()

    override fun fillAudioFrame(frame: RawFrame): RawFrame {
        return frame
    }

    override fun getAudioFrame(frameFactory: IReadOnlyRawFrameFactory) =
        frameFactory.create(8192, 0)

    override suspend fun startStream() {
        _isStreamingFlow.value = true
    }

    override suspend fun stopStream() {
        _isStreamingFlow.value = false
    }

    override suspend fun configure(config: AudioSourceConfig) {
        _configurationFlow.value = config
    }


    override fun release() {

    }

    class Factory : IAudioSourceInternal.Factory {
        override suspend fun create(context: Context): IAudioSourceInternal {
            return StubAudioSource()
        }

        override fun isSourceEquals(source: IAudioSourceInternal?): Boolean {
            return source is StubAudioSource
        }
    }
}