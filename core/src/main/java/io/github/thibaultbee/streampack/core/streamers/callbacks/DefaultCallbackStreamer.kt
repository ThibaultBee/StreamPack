package io.github.thibaultbee.streampack.core.streamers.callbacks

import android.Manifest
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.encoders.IPublicEncoder
import io.github.thibaultbee.streampack.core.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.core.internal.sources.audio.IPublicAudioSource
import io.github.thibaultbee.streampack.core.internal.sources.video.IPublicVideoSource
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Default implementation of [ICallbackStreamer] that uses [ICoroutineStreamer] to handle streamer logic.
 * It is a bridge between [ICoroutineStreamer] and [ICallbackStreamer].
 *
 * @param streamer the [ICoroutineStreamer] to use
 */
class DefaultCallbackStreamer(val streamer: ICoroutineStreamer) : ICallbackStreamer {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val listeners = mutableListOf<ICallbackStreamer.Listener>()

    override val isOpened: Boolean
        get() = streamer.isOpened.value
    override val isStreaming: Boolean
        get() = streamer.isStreaming.value

    override val audioSource: IPublicAudioSource?
        get() = streamer.audioSource
    override val audioEncoder: IPublicEncoder?
        get() = streamer.audioEncoder
    override val videoSource: IPublicVideoSource?
        get() = streamer.videoSource
    override val videoEncoder: IPublicEncoder?
        get() = streamer.videoEncoder
    override val endpoint: IPublicEndpoint
        get() = streamer.endpoint
    override val info: IConfigurationInfo
        get() = streamer.info

    init {
        coroutineScope.launch {
            streamer.exception.collect { e ->
                if (e != null) {
                    listeners.forEach { it.onError(e) }
                }
            }
        }
        coroutineScope.launch {
            streamer.isOpened.collect { isOpened ->
                listeners.forEach { it.onIsOpenChanged(isOpened) }
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
    override fun configure(audioConfig: AudioConfig) {
        streamer.configure(audioConfig)
    }

    override fun configure(videoConfig: VideoConfig) {
        streamer.configure(videoConfig)
    }

    override fun open(descriptor: MediaDescriptor) {
        coroutineScope.launch {
            try {
                streamer.open(descriptor)
            } catch (e: Exception) {
                listeners.forEach { it.onIsOpenFailed(e) }
            }
        }
    }

    override fun close() {
        coroutineScope.launch {
            try {
                streamer.close()
            } catch (e: Exception) {
                listeners.forEach { it.onError(e) }
            }
        }
    }

    override fun startStream() {
        coroutineScope.launch {
            try {
                streamer.startStream()
            } catch (e: Exception) {
                listeners.forEach { it.onError(e) }
            }
        }
    }

    override fun startStream(descriptor: MediaDescriptor) {
        coroutineScope.launch {
            try {
                streamer.startStream(descriptor)
            } catch (e: Exception) {
                listeners.forEach { it.onError(e) }
            }
        }
    }

    override fun stopStream() {
        coroutineScope.launch {
            try {
                streamer.stopStream()
            } catch (e: Exception) {
                listeners.forEach { it.onError(e) }
            }
        }
    }

    override fun addListener(listener: ICallbackStreamer.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ICallbackStreamer.Listener) {
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