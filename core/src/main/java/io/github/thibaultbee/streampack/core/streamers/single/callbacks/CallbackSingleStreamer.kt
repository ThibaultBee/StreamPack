package io.github.thibaultbee.streampack.core.streamers.single.callbacks

import android.Manifest
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackAudioSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICoroutineSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.utils.extensions.isClosedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Default implementation of [ICallbackSingleStreamer] that uses [ICoroutineSingleStreamer] to handle streamer logic.
 * It is a bridge between [ICoroutineSingleStreamer] and [ICallbackSingleStreamer].
 *
 * @param streamer the [ICoroutineSingleStreamer] to use
 */
open class CallbackSingleStreamer(val streamer: SingleStreamer) :
    ICallbackSingleStreamer, ICallbackAudioSingleStreamer, ICallbackVideoSingleStreamer {
    protected val coroutineScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    protected val listeners = mutableListOf<ICallbackSingleStreamer.Listener>()

    override val audioConfig: AudioConfig?
        get() = streamer.audioConfig

    override val videoConfig: VideoConfig?
        get() = streamer.videoConfig

    override val audioSource: IAudioSource?
        get() = streamer.audioSource
    override val audioProcessor = streamer.audioProcessor
    override val audioEncoder: IEncoder?
        get() = streamer.audioEncoder
    override val videoSource: IVideoSource?
        get() = streamer.videoSource
    override val videoEncoder: IEncoder?
        get() = streamer.videoEncoder
    override val endpoint: IEndpoint
        get() = streamer.endpoint

    override val info: IConfigurationInfo
        get() = streamer.info

    override val isOpen: Boolean
        get() = streamer.isOpenFlow.value
    override val isStreaming: Boolean
        get() = streamer.isStreamingFlow.value

    override var targetRotation: Int
        @RotationValue
        get() = streamer.targetRotation
        set(@RotationValue value) {
            streamer.targetRotation = value
        }

    init {
        coroutineScope.launch {
            streamer.throwableFlow.filterNotNull().filter { !it.isClosedException }.collect { e ->
                listeners.forEach { it.onError(e) }
            }
        }
        coroutineScope.launch {
            streamer.throwableFlow.filterNotNull().filter { it.isClosedException }.collect { e ->
                listeners.forEach { it.onClose(e) }
            }
        }
        coroutineScope.launch {
            // Skip first value to avoid duplicate event
            streamer.isOpenFlow.drop(1).collect { isOpen ->
                listeners.forEach { it.onIsOpenChanged(isOpen) }
            }
        }
        coroutineScope.launch {
            streamer.isStreamingFlow.collect { isStreaming ->
                listeners.forEach { it.onIsStreamingChanged(isStreaming) }
            }
        }
    }

    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        return streamer.getInfo(descriptor)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun setAudioConfig(audioConfig: AudioConfig) {
        coroutineScope.launch {
            streamer.setAudioConfig(audioConfig)
        }
    }

    override fun setVideoConfig(videoConfig: VideoConfig) {
        coroutineScope.launch {
            streamer.setVideoConfig(videoConfig)
        }
    }

    /**
     * Configures both video and audio settings.
     * It is the first method to call after a [SingleStreamer] instantiation.
     * It must be call when both stream and audio and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     * @see [ISingleStreamer.release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun setConfig(audioConfig: AudioConfig, videoConfig: VideoConfig) {
        setAudioConfig(audioConfig)
        setVideoConfig(videoConfig)
    }

    override fun open(descriptor: MediaDescriptor) {
        coroutineScope.launch {
            try {
                streamer.open(descriptor)
            } catch (t: Throwable) {
                listeners.forEach { it.onOpenFailed(t) }
            }
        }
    }

    override fun close() {
        coroutineScope.launch {
            try {
                streamer.close()
            } catch (t: Throwable) {
                listeners.forEach { it.onError(t) }
            }
        }
    }

    override fun startStream() {
        coroutineScope.launch {
            try {
                streamer.startStream()
            } catch (t: Throwable) {
                listeners.forEach { it.onError(t) }
            }
        }
    }

    override fun startStream(descriptor: MediaDescriptor) {
        coroutineScope.launch {
            try {
                streamer.open(descriptor)
            } catch (t: Throwable) {
                listeners.forEach { it.onOpenFailed(t) }
                return@launch
            }
            try {
                streamer.startStream()
            } catch (t: Throwable) {
                listeners.forEach { it.onError(t) }
            }
        }
    }

    override fun stopStream() {
        coroutineScope.launch {
            try {
                streamer.stopStream()
            } catch (t: Throwable) {
                listeners.forEach { it.onError(t) }
            }
        }
    }

    override fun addListener(listener: ICallbackSingleStreamer.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ICallbackSingleStreamer.Listener) {
        listeners.remove(listener)
    }

    override fun release() {
        streamer.releaseBlocking()
        listeners.clear()
        coroutineScope.coroutineContext.cancelChildren()
    }

    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) {
        streamer.addBitrateRegulatorController(controllerFactory)
    }

    override fun removeBitrateRegulatorController() {
        streamer.removeBitrateRegulatorController()
    }
}