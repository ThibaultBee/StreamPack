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
import java.util.concurrent.atomic.AtomicBoolean


/**
 * The public interface for the audio input.
 * It provides access to the audio source, the audio processor, and the streaming state.
 */
interface IAudioInput {
    /**
     * Whether the audio input is streaming.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * The audio source.
     * It allows access to advanced audio settings.
     *
     * It is a shortcut to [IAudioFrameProcessor.isMuted]
     */
    var isMuted: Boolean
        get() = processor.isMuted
        set(value) {
            processor.isMuted = value
        }

    /**
     * Whether the pipeline has an audio source.
     */
    val withSource: Boolean
        get() = sourceFlow.value != null

    /**
     * The audio source
     */
    val sourceFlow: StateFlow<IAudioSource?>

    /**
     * Sets a new audio source.
     *
     * @param audioSourceFactory The new audio source factory.
     */
    suspend fun setSource(audioSourceFactory: IAudioSourceInternal.Factory)

    /**
     * Whether the audio input has a configuration.
     * It is true if the audio source has been configured.
     */
    val withConfig: Boolean

    /**
     * The audio processor for adding effects to the audio frames.
     */
    val processor: IAudioFrameProcessor
}

/**
 * A internal class that manages an audio source and an audio processor.
 */
internal class AudioInput(
    private val context: Context,
    config: Config,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IAudioInput {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private var isStreamingJob = ConflatedJob()

    private var isReleaseRequested = AtomicBoolean(false)

    private val audioSourceMutex = Mutex()

    // SOURCE
    private var sourceInternalFlow = MutableStateFlow<IAudioSourceInternal?>(null)

    /**
     * The audio source.
     * It allows access to advanced audio settings.
     */
    override val sourceFlow: StateFlow<IAudioSource?> = sourceInternalFlow.asStateFlow()

    val frameRequestedListener: OnFrameRequestedListener
        get() {
            return if (port is CallbackAudioPort) {
                port.audioFrameRequestedListener
            } else {
                throw IllegalStateException("Audio frame requested listener is not supported in this mode: ${port::class.java}")
            }
        }

    // PROCESSOR
    /**
     * The audio processor.
     */
    private val frameProcessorInternal = AudioFrameProcessor()
    override val processor: IAudioFrameProcessor = frameProcessorInternal
    private val port = if (config is PushConfig) {
        PushAudioPort(frameProcessorInternal, config)
    } else {
        CallbackAudioPort(frameProcessorInternal)
    }

    // CONFIG
    private val _sourceConfigFlow = MutableStateFlow<AudioSourceConfig?>(null)

    /**
     * The audio source configuration.
     */
    val sourceConfigFlow = _sourceConfigFlow.asStateFlow()

    private val sourceConfig: AudioSourceConfig?
        get() = sourceConfigFlow.value

    override val withConfig: Boolean
        get() = sourceConfig != null

    // STATE
    /**
     * Whether the audio input is streaming.
     */
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    /**
     * Sets a new audio source.
     *
     * @param audioSourceFactory The new audio source factory.
     */
    override suspend fun setSource(audioSourceFactory: IAudioSourceInternal.Factory) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                val previousAudioSource = sourceInternalFlow.value
                val isStreaming = previousAudioSource?.isStreamingFlow?.value ?: false

                if (audioSourceFactory.isSourceEquals(previousAudioSource)) {
                    Logger.i(TAG, "Audio source is already set, skipping")
                    return@withContext
                }

                // Prepare new video source
                val newAudioSource = audioSourceFactory.create(context)

                sourceConfig?.let { newAudioSource.configure(it) }
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

                when (port) {
                    is PushAudioPort -> {
                        port.setInput(newAudioSource::getAudioFrame)
                    }

                    is CallbackAudioPort -> {
                        port.setInput(newAudioSource::fillAudioFrame)
                    }
                }

                // Replace audio source
                sourceInternalFlow.emit(newAudioSource)

                // Stop previous audio source
                try {
                    previousAudioSource?.stopStream()
                    previousAudioSource?.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "setAudioSource: Can't stop previous audio source: ${t.message}")
                }
            }
        }
    }


    suspend fun setSourceConfig(newAudioSourceConfig: AudioSourceConfig) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }

        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                if (sourceConfig == newAudioSourceConfig) {
                    Logger.i(TAG, "Audio source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change audio source configuration while streaming" }

                try {
                    sourceInternalFlow.value?.let {
                        applySourceConfig(
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
                    _sourceConfigFlow.emit(newAudioSourceConfig)
                }
            }
        }
    }

    private suspend fun applySourceConfig(
        audioSource: IAudioSourceInternal,
        audioConfig: AudioSourceConfig
    ) {
        audioSource.configure(audioConfig)
    }

    suspend fun startStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                val source = requireNotNull(sourceInternalFlow.value) {
                    "Audio source is not set yet"
                }
                if (isStreamingFlow.value) {
                    Logger.w(TAG, "Stream is already running")
                    return@withContext
                }
                if (!withConfig) {
                    Logger.w(TAG, "Audio source configuration is not set yet")
                }
                source.startStream()
                try {
                    port.startStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "startStream: Can't start audio processor: ${t.message}")
                    source.stopStream()
                    throw t
                }
                _isStreamingFlow.emit(true)
            }
        }
    }

    suspend fun stopStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                _isStreamingFlow.emit(false)
                try {
                    port.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop audio processor: ${t.message}")
                }
                try {
                    sourceInternalFlow.value?.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop audio source: ${t.message}")
                }
            }
        }
    }

    suspend fun release() {
        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
            return
        }

        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                _isStreamingFlow.emit(false)
                try {
                    port.removeInput()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't remove audio processor input: ${t.message}")
                }
                try {
                    port.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't release audio processor: ${t.message}")
                }
                try {
                    sourceInternalFlow.value?.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't release audio source: ${t.message}")
                }

                isStreamingJob.cancel()
            }
            coroutineScope.coroutineContext.cancelChildren()
        }
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

    var audioFrameRequestedListener: OnFrameRequestedListener =
        object : OnFrameRequestedListener {
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