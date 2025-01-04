package io.github.thibaultbee.streampack.core.elements.sources

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class StubAudioSource : IAudioSourceInternal {
    override var isMuted: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _configurationFlow = MutableStateFlow<AudioSourceConfig?>(null)
    val configurationFlow = _configurationFlow.asStateFlow()

    override fun getAudioFrame(inputBuffer: ByteBuffer?): Frame {
        return Frame(
            inputBuffer ?: ByteBuffer.allocate(8192),
            0,
            format = MediaFormat().apply {
                setString(
                    MediaFormat.KEY_MIME,
                    MediaFormat.MIMETYPE_AUDIO_RAW
                )
            })
    }

    override fun startStream() {
        _isStreamingFlow.value = true
    }

    override fun stopStream() {
        _isStreamingFlow.value = false
    }

    override fun configure(config: AudioSourceConfig) {
        _configurationFlow.value = config
    }


    override fun release() {

    }
}