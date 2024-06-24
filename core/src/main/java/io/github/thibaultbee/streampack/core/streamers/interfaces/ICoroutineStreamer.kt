/*
 * Copyright (C) 2021 Thibault B.
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

import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import kotlinx.coroutines.flow.StateFlow

/**
 * A Streamer based on coroutines.
 */
interface ICoroutineStreamer : IStreamer {
    /**
     * Returns the last throwable that occurred.
     */
    val throwable: StateFlow<Throwable?>

    /**
     * Returns true if endpoint is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpened: StateFlow<Boolean>

    /**
     * Returns true if stream is running.
     */
    val isStreaming: StateFlow<Boolean>

    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    suspend fun open(descriptor: MediaDescriptor)

    /**
     * Closes the streamer endpoint.
     */
    suspend fun close()

    /**
     * Starts audio/video stream.
     *
     * @see [stopStream]
     */
    suspend fun startStream()

    /**
     * Starts audio/video stream.
     *
     * Same as doing [open] and [startStream].
     *
     * @see [stopStream]
     */
    suspend fun startStream(descriptor: MediaDescriptor) {
        open(descriptor)
        startStream()
    }

    /**
     * Stops audio/video stream.
     *
     * @see [startStream]
     */
    suspend fun stopStream()
}