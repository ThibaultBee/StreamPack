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
package io.github.thibaultbee.streampack.streamers.file

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.internal.endpoints.IFileEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.composites.FileCompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.endpoints.sinks.FileSink
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import java.io.File
import java.io.FileDescriptor
import java.io.OutputStream

/**
 * A [BaseCameraStreamer] that sends microphone and camera frames to a [File].
 *
 * @param context application context
 * @param muxer the [IMuxer] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 */
open class BaseCameraFileStreamer(
    context: Context,
    enableAudio: Boolean = true,
    muxer: IMuxer,
) : BaseCameraStreamer(
    context = context,
    enableAudio = enableAudio,
    internalEndpoint = FileCompositeEndpoint(muxer, FileSink())
),
    IFileStreamer {
    private val fileEndpoint = internalEndpoint as IFileEndpoint

    /**
     * Get/Set file.
     * To set an [OutputStream] instead, see [outputStream].
     *
     * @see [outputStream]
     */
    override var file: File?
        /**
         * Get registered [File].
         *
         * @return file where to write the stream
         */
        get() = fileEndpoint.file
        /**
         * Set [File].
         *
         * @param value [File] where to write the stream
         */
        set(value) {
            fileEndpoint.file = value
        }

    /**
     * Get/Set outputStream. outputStream will be closed on [stopStream].
     * To set an [File] instead, see [file].
     *
     * @see file
     */
    override var outputStream: OutputStream?
        /**
         * Get registered [OutputStream].
         *
         * @return file where to write the stream
         */
        get() = fileEndpoint.outputStream
        /**
         * Set [OutputStream].
         *
         * @param value [OutputStream] to write the stream
         */
        set(value) {
            fileEndpoint.outputStream = value
        }

    /**
     * Get/Set fileDescriptor.
     */
    override var fileDescriptor: FileDescriptor?
        /**
         * Get registered [FileDescriptor].
         */
        get() = fileEndpoint.fileDescriptor
        /**
         * Set [FileDescriptor].
         */
        set(value) {
            fileEndpoint.fileDescriptor = value
        }

    /**
     * Same as [BaseCameraStreamer.startStream] with RequiresPermission annotation for
     * Manifest.permission.WRITE_EXTERNAL_STORAGE.
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override suspend fun startStream() = super.startStream()
}