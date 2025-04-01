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
package io.github.thibaultbee.streampack.core.interfaces

import android.net.Uri
import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * A streamer based on coroutines.
 */
interface IStreamer {
    /**
     * Returns the last throwable that occurred.
     */
    val throwableFlow: StateFlow<Throwable?>

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
 * @see [IStreamer.release]
 */
fun IStreamer.releaseBlocking() = runBlocking {
    release()
}

/**
 * An interface for a component that can be closed.
 */
interface ICloseableStreamer : IStreamer {
    /**
     * Returns true if output is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpenFlow: StateFlow<Boolean>

    /**
     * Closes the streamer endpoint.
     *
     * @see [open]
     */
    suspend fun close()
}

/**
 * An interface for a component that can be opened.
 */
interface IOpenableStreamer : ICloseableStreamer {
    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     *
     * @see [close]
     */
    suspend fun open(descriptor: MediaDescriptor)
}

/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
suspend fun IOpenableStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
suspend fun IOpenableStreamer.open(uriString: String) =
    open(UriMediaDescriptor(uriString.toUri()))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param descriptor The media descriptor to open
 * @see [IOpenableStreamer.stopStream]
 */
suspend fun IOpenableStreamer.startStream(descriptor: MediaDescriptor) {
    open(descriptor)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [IOpenableStreamer.stopStream]
 */
suspend fun IOpenableStreamer.startStream(uri: Uri) {
    open(uri)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [IOpenableStreamer.stopStream]
 */
suspend fun IOpenableStreamer.startStream(uriString: String) {
    open(uriString)
    try {
        startStream()
    } catch (t: Throwable) {
        close()
        throw t
    }
}