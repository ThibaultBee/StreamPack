package io.github.thibaultbee.streampack.core.streamers.single.callbacks

import android.Manifest
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.encoders.IEncoder
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.internal.utils.RotationValue
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICoroutineSingleStreamer
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
open class CallbackSingleStreamer(val streamer: ICoroutineSingleStreamer) :
    ICallbackSingleStreamer {
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
        get() = streamer.isOpen.value
    override val isStreaming: Boolean
        get() = streamer.isStreaming.value

    override var targetRotation: Int
        @RotationValue
        get() = streamer.targetRotation
        set(@RotationValue value) {
            streamer.targetRotation = value
        }

    init {
        coroutineScope.launch {
            streamer.throwable.filterNotNull().filter { !it.isClosedException }.collect { e ->
                listeners.forEach { it.onError(e) }
            }
        }
        coroutineScope.launch {
            streamer.throwable.filterNotNull().filter { it.isClosedException }.collect { e ->
                listeners.forEach { it.onClose(e) }
            }
        }
        coroutineScope.launch {
            // Skip first value to avoid duplicate event
            streamer.isOpen.drop(1).collect { isOpen ->
                listeners.forEach { it.onIsOpenChanged(isOpen) }
            }
        }
        coroutineScope.launch {
            streamer.isStreaming.collect { isStreaming ->
                listeners.forEach { it.onIsStreamingChanged(isStreaming) }
            }
        }
    }

    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        return streamer.getInfo(descriptor)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun setAudioConfig(audioConfig: AudioConfig) {
        streamer.setAudioConfig(audioConfig)
    }

    override fun setVideoConfig(videoConfig: VideoConfig) {
        streamer.setVideoConfig(videoConfig)
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
        streamer.release()
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