package io.github.thibaultbee.streampack.core.elements.sources

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class StubAudioSource : IAudioSourceInternal {
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _configurationFlow = MutableStateFlow<AudioSourceConfig?>(null)
    val configurationFlow = _configurationFlow.asStateFlow()

    override val minBufferSize = 0
    override fun fillAudioFrame(buffer: ByteBuffer): Long {
        return 0L
    }

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