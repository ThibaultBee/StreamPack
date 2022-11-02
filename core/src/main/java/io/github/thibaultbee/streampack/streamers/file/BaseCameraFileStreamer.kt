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
import io.github.thibaultbee.streampack.internal.endpoints.FileWriter
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import java.io.File
import java.io.OutputStream

/**
 * A [BaseCameraStreamer] that sends microphone and camera frames to a [File].
 *
 * @param context application context
 * @param muxer a [IMuxer] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
open class BaseCameraFileStreamer(
    context: Context,
    enableAudio: Boolean = true,
    muxer: IMuxer,
    initialOnErrorListener: OnErrorListener? = null
) : BaseCameraStreamer(
    context = context,
    enableAudio = enableAudio,
    muxer = muxer,
    endpoint = FileWriter(),
    initialOnErrorListener = initialOnErrorListener
),
    IFileStreamer {
    private val fileWriter = endpoint as FileWriter

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
        get() = fileWriter.file
        /**
         * Set [File].
         *
         * @param value [File] where to write the stream
         */
        set(value) {
            fileWriter.file = value
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
        get() = fileWriter.outputStream
        /**
         * Set [OutputStream].
         *
         * @param value [OutputStream] to write the stream
         */
        set(value) {
            fileWriter.outputStream = value
        }

    /**
     * Same as [BaseCameraStreamer.startStream] with RequiresPermission annotation for
     * Manifest.permission.WRITE_EXTERNAL_STORAGE.
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun startStream() = super.startStream()
}