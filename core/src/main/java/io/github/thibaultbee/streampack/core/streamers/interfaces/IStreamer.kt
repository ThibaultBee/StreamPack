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
package io.github.thibaultbee.streampack.core.streamers.interfaces

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.streamers.single.open
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

interface IStreamer

/**
 * A single Streamer based on coroutines.
 */
interface ICoroutineStreamer : IStreamer {
    /**
     * Returns the last throwable that occurred.
     */
    val throwableFlow: StateFlow<Throwable?>

    /**
     * Returns true if the streamer is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpenFlow: StateFlow<Boolean>

    /**
     * Closes the streamer.
     */
    suspend fun close()

    /**
     * Returns true if stream is running.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * Starts audio/video stream.
     *
     * @see [stopStream]
     */
    suspend fun startStream()

    /**
     * Stops audio/video stream.
     *
     * @see [startStream]
     */
    suspend fun stopStream()

    /**
     * Clean and reset the streamer.
     */
    suspend fun release()
}

/**
 * Clean and reset the streamer synchronously.
 *
 * @see [ICoroutineStreamer.release]
 */
fun ICoroutineStreamer.releaseBlocking() = runBlocking {
    release()
}


interface ICoroutineAudioStreamer<T> {
    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setAudioConfig(audioConfig: T)
}

interface ICoroutineVideoStreamer<T> {
    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setVideoConfig(videoConfig: T)
}

/**
 * An audio single Streamer
 */
interface IAudioStreamer {

    /**
     * Advanced settings for the audio source.
     */
    val audioSource: IAudioSource?

    /**
     * Advanced settings for the audio processor.
     */
    val audioProcessor: IAudioFrameProcessor?
}

/**
 * A video single streamer.
 */
interface IVideoStreamer {
    /**
     * Advanced settings for the video source.
     */
    val videoSource: IVideoSource?
}


interface ICallbackAudioStreamer<T> {
    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    fun setAudioConfig(audioConfig: T)
}

interface ICallbackVideoStreamer<T> {
    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    fun setVideoConfig(videoConfig: T)
}

interface ICallbackStreamer : IStreamer {
    /**
     * Returns true if streamer is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpen: Boolean

    /**
     * Closes the streamer.
     */
    fun close()

    /**
     * Returns true if stream is running.
     */
    val isStreaming: Boolean

    /**
     * Starts audio/video stream asynchronously.
     *
     * You must call [open] before calling this method.
     * The streamer must be opened before starting the stream. You can use [Listener.onIsOpenChanged].
     *
     * @see [stopStream]
     */
    fun startStream()

    /**
     * Starts audio/video stream asynchronously.
     *
     * Same as doing [open] and [startStream].
     *
     * @see [stopStream]
     */
    fun startStream(descriptor: MediaDescriptor)

    /**
     * Stops audio/video stream asynchronously.
     *
     * @see [startStream]
     */
    fun stopStream()

    /**
     * Clean and reset the streamer.
     */
    fun release()

    /**
     * Listener for the callback streamer.
     */
    interface Listener {
        /**
         * Called when the streamer is opened or closed.
         */
        fun onIsOpenChanged(isOpen: Boolean) = Unit

        /**
         * Called when the stream is started or stopped.
         */
        fun onIsStreamingChanged(isStarted: Boolean) = Unit

        /**
         * Called when an error occurs.
         *
         * @param throwable The throwable that occurred
         */
        fun onError(throwable: Throwable) = Unit
    }
}