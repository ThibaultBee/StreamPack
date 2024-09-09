/*
 * Copyright (C) 2022 Thibault B.
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

package io.github.thibaultbee.streampack.core.streamers

import android.net.Uri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IStreamer

/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
suspend fun ICoroutineStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
suspend fun ICoroutineStreamer.open(uriString: String) =
    open(UriMediaDescriptor(Uri.parse(uriString)))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param descriptor The media descriptor to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(descriptor: MediaDescriptor) {
    open(descriptor)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(uri: Uri) {
    open(uri)
    startStream()
}

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [stopStream]
 */
suspend fun ICoroutineStreamer.startStream(uriString: String) {
    open(uriString)
    startStream()
}

/**
 * Opens the streamer endpoint.
 *
 * @param uri The uri to open
 */
fun ICallbackStreamer.open(uri: Uri) =
    open(UriMediaDescriptor(uri))

/**
 * Opens the streamer endpoint.
 *
 * @param uriString The uri to open
 */
fun ICallbackStreamer.open(uriString: String) =
    open(UriMediaDescriptor(Uri.parse(uriString)))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uri The uri to open
 * @see [stopStream]
 */
fun ICallbackStreamer.startStream(uri: Uri) = startStream(UriMediaDescriptor(uri))

/**
 * Starts audio/video stream.
 *
 * Same as doing [open] and [startStream].
 *
 * @param uriString The uri to open
 * @see [stopStream]
 */
fun ICallbackStreamer.startStream(uriString: String) = startStream(Uri.parse(uriString))

/**
 * Get a streamer if it from generic class or interface
 */
inline fun <reified T> IStreamer.getStreamer(): T? {
    return if (this is T) {
        this
    } else {
        null
    }
}

/**
 * Casts streamer to [ICameraStreamer].
 */
fun IStreamer.getCameraStreamer(): ICameraStreamer? = getStreamer<ICameraStreamer>()