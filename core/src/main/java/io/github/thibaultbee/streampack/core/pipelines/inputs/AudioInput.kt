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
package io.github.thibaultbee.streampack.core.pipelines.inputs

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.interfaces.Streamable
import io.github.thibaultbee.streampack.core.elements.processing.RawFramePullPush
import io.github.thibaultbee.streampack.core.elements.processing.audio.AudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.ConflatedJob
import io.github.thibaultbee.streampack.core.elements.utils.pool.IRawFrameFactory
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.inputs.AudioInput.PushConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * A internal class that manages an audio source and an audio processor.
 */
internal class AudioInput(
    private val context: Context,
    config: Config,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private var isStreamingJob = ConflatedJob()

    private val audioSourceMutex = Mutex()

    // SOURCE
    private var audioSourceInternalFlow = MutableStateFlow<IAudioSourceInternal?>(null)

    /**
     * Whether the pipeline has an audio source.
     */
    val hasSource: Boolean
        get() = audioSourceInternalFlow.value != null

    /**
     * The audio source.
     * It allows access to advanced audio settings.
     */
    val audioSourceFlow: StateFlow<IAudioSource?> = audioSourceInternalFlow.asStateFlow()

    val audioFrameRequestedListener: OnFrameRequestedListener
        get() {
            return if (audioPort is CallbackAudioPort) {
                audioPort.audioFrameRequestedListener
            } else {
                throw IllegalStateException("Audio frame requested listener is not supported in this mode: ${audioPort::class.java}")
            }
        }

    // PROCESSOR
    /**
     * The audio processor.
     */
    private val audioFrameProcessorInternal = AudioFrameProcessor()
    val audioProcessor: IAudioFrameProcessor = audioFrameProcessorInternal
    private val audioPort = if (config is PushConfig) {
        PushAudioPort(audioFrameProcessorInternal, config)
    } else {
        CallbackAudioPort(audioFrameProcessorInternal)
    }

    // CONFIG
    private val _audioSourceConfigFlow = MutableStateFlow<AudioSourceConfig?>(null)

    /**
     * The audio source configuration.
     */
    val audioSourceConfigFlow = _audioSourceConfigFlow.asStateFlow()

    private val audioSourceConfig: AudioSourceConfig?
        get() = audioSourceConfigFlow.value

    val hasConfig: Boolean
        get() = audioSourceConfigFlow.value != null

    // STATE
    /**
     * Whether the audio input is streaming.
     */
    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingFlow = _isStreamingFlow.asStateFlow()

    /**
     * Sets a new audio source.
     *
     * @param audioSourceFactory The new audio source factory.
     */
    suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) =
        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                val previousAudioSource = audioSourceInternalFlow.value
                val isStreaming = previousAudioSource?.isStreamingFlow?.value ?: false

                if (audioSourceFactory.isSourceEquals(previousAudioSource)) {
                    Logger.i(TAG, "Audio source is already set, skipping")
                    return@withContext
                }

                // Prepare new video source
                val newAudioSource = audioSourceFactory.create(context)

                audioSourceConfig?.let { newAudioSource.configure(it) }
                if (isStreaming) {
                    newAudioSource.startStream()
                }

                isStreamingJob += coroutineScope.launch {
                    newAudioSource.isStreamingFlow.collect { isStreaming ->
                        if ((!isStreaming) && isStreamingFlow.value) {
                            Logger.i(TAG, "Audio source has been stopped.")
                            stopStream()
                        }
                    }
                }

                when (audioPort) {
                    is PushAudioPort -> {
                        audioPort.setInput(newAudioSource::getAudioFrame)
                    }

                    is CallbackAudioPort -> {
                        audioPort.setInput(newAudioSource::fillAudioFrame)
                    }
                }

                // Replace audio source
                audioSourceInternalFlow.emit(newAudioSource)

                // Stop previous audio source
                try {
                    previousAudioSource?.stopStream()
                    previousAudioSource?.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "setAudioSource: Can't stop previous audio source: ${t.message}")
                }
            }
        }


    suspend fun setAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig) =
        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                if (audioSourceConfig == newAudioSourceConfig) {
                    Logger.i(TAG, "Audio source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change audio source configuration while streaming" }

                try {
                    audioSourceInternalFlow.value?.let {
                        applyAudioSourceConfig(
                            it,
                            newAudioSourceConfig
                        )
                    } ?: Logger.w(
                        TAG,
                        "setAudioSourceConfig: Audio source is not set yet"
                    )
                } catch (t: Throwable) {
                    throw t
                } finally {
                    _audioSourceConfigFlow.emit(newAudioSourceConfig)
                }
            }
        }

    private suspend fun applyAudioSourceConfig(
        audioSource: IAudioSourceInternal,
        audioConfig: AudioSourceConfig
    ) {
        audioSource.configure(audioConfig)
    }

    suspend fun startStream() = withContext(coroutineDispatcher) {
        audioSourceMutex.withLock {
            val source = requireNotNull(audioSourceInternalFlow.value) {
                "Audio source is not set yet"
            }
            if (isStreamingFlow.value) {
                Logger.w(TAG, "Stream is already running")
                return@withContext
            }
            if (!hasConfig) {
                Logger.w(TAG, "Audio source configuration is not set yet")
            }
            source.startStream()
            try {
                audioPort.startStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "startStream: Can't start audio processor: ${t.message}")
                source.stopStream()
                throw t
            }
            _isStreamingFlow.emit(true)
        }
    }

    suspend fun stopStream() = withContext(coroutineDispatcher) {
        audioSourceMutex.withLock {
            _isStreamingFlow.emit(false)
            try {
                audioPort.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio processor: ${t.message}")
            }
            try {
                audioSourceInternalFlow.value?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio source: ${t.message}")
            }
        }
    }

    suspend fun release() = withContext(coroutineDispatcher) {
        audioSourceMutex.withLock {
            _isStreamingFlow.emit(false)
            try {
                audioPort.removeInput()
            } catch (t: Throwable) {
                Logger.w(TAG, "release: Can't remove audio processor input: ${t.message}")
            }
            try {
                audioPort.release()
            } catch (t: Throwable) {
                Logger.w(TAG, "release: Can't release audio processor: ${t.message}")
            }
            try {
                audioSourceInternalFlow.value?.release()
            } catch (t: Throwable) {
                Logger.w(TAG, "release: Can't release audio source: ${t.message}")
            }

            isStreamingJob.cancel()
        }
        coroutineScope.coroutineContext.cancelChildren()
    }

    companion object {
        private const val TAG = "AudioInput"
    }

    internal sealed class Config

    internal class PushConfig(val onFrame: (RawFrame) -> Unit) : Config()
    internal class CallbackConfig : Config()
}

private sealed interface IAudioPort<T> : Streamable, Releasable {
    fun setInput(getFrame: T)
    fun removeInput()
}

private class PushAudioPort(
    audioFrameProcessor: AudioFrameProcessor,
    config: PushConfig,
) : IAudioPort<(frameFactory: IRawFrameFactory) -> RawFrame> {
    private val audioPullPush = RawFramePullPush(audioFrameProcessor, config.onFrame)

    override fun setInput(getFrame: (frameFactory: IRawFrameFactory) -> RawFrame) {
        audioPullPush.setInput(getFrame)
    }

    override fun removeInput() {
        audioPullPush.removeInput()
    }

    override fun startStream() {
        audioPullPush.startStream()
    }

    override fun stopStream() {
        audioPullPush.stopStream()
    }

    override fun release() {
        audioPullPush.release()
    }
}

private class CallbackAudioPort(private val audioFrameProcessor: AudioFrameProcessor) :
    IAudioPort<(frame: RawFrame) -> RawFrame> {
    private var getFrame: ((frame: RawFrame) -> RawFrame)? = null

    var audioFrameRequestedListener: OnFrameRequestedListener = object : OnFrameRequestedListener {
        override fun onFrameRequested(buffer: ByteBuffer): RawFrame {
            val getFrame = requireNotNull(getFrame) {
                "Audio frame requested listener is not set yet"
            }
            val frame = getFrame(RawFrame(buffer, 0))
            return audioFrameProcessor.processFrame(frame)
        }
    }

    override fun setInput(getFrame: (frame: RawFrame) -> RawFrame) {
        synchronized(this) {
            this.getFrame = getFrame
        }
    }

    override fun removeInput() {
        synchronized(this) {
            this.getFrame = null
        }
    }

    override fun startStream() = Unit

    override fun stopStream() = Unit

    override fun release() = Unit
}