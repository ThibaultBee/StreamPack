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

interface ICallbackStreamer : IStreamer {
    /**
     * Returns true if endpoint is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpened: Boolean

    /**
     * Returns true if stream is running.
     */
    val isStreaming: Boolean

    /**
     * Opens the streamer endpoint asynchronously.
     *
     * @param descriptor Media descriptor to open
     */
    fun open(descriptor: MediaDescriptor)

    /**
     * Closes the streamer endpoint.
     */
    fun close()

    /**
     * Starts audio/video stream asynchronously.
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
     * Adds a listener to the streamer.
     */
    fun addListener(listener: Listener)

    /**
     * Removes a listener from the streamer.
     */
    fun removeListener(listener: Listener)

    interface Listener {
        /**
         * Called when the streamer is opened or closed.
         */
        fun onIsOpenChanged(isOpened: Boolean)

        /**
         * Called when the streamer opening failed.
         *
         * @param e The exception that occurred
         */
        fun onIsOpenFailed(e: Exception)

        /**
         * Called when the stream is started or stopped.
         */
        fun onIsStreamingChanged(isStarted: Boolean)

        /**
         * Called when an error occurs.
         *
         * @param exception The exception that occurred
         */
        fun onError(exception: Exception)
    }
}