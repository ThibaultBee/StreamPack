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
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import java.io.File

/**
 * A [BaseCameraStreamer] that sends microphone and camera frames to a [File].
 *
 * @param context application context
 * @param muxer a [IMuxer] implementation
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 */
open class BaseCameraFileStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
    muxer: IMuxer,
) : BaseCameraStreamer(
    context = context,
    logger = logger,
    enableAudio = enableAudio,
    muxer = muxer,
    endpoint = FileWriter(logger)
),
    IFileStreamer {
    private val fileWriter = endpoint as FileWriter

    /**
     * Get/Set [FileWriter] file. If no file has been set. [FileWriter] uses a default temporary file.
     */
    override var file: File?
        /**
         * Get file writer file.
         *
         * @return file where [FileWriter] writes
         */
        get() = fileWriter.file
        /**
         * Set file writer file.
         *
         * @param value [File] where [FileWriter] writes
         */
        set(value) {
            fileWriter.file = value
        }

    /**
     * Same as [BaseCameraStreamer.startStream] with RequiresPermission annotation for
     * Manifest.permission.WRITE_EXTERNAL_STORAGE.
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun startStream() = super.startStream()

    abstract class Builder : BaseCameraStreamer.Builder(), IFileStreamerBuilder {
        protected var file: File? = null

        /**
         * Set destination file.
         *
         * @param file where to write date
         */
        override fun setFile(file: File) = apply {
            this.file = file
        }

        /**
         * Combines all of the characteristics that have been set and return a new
         * generic [BaseCameraFileStreamer] object.
         *
         * @return a new generic [BaseCameraFileStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): BaseCameraFileStreamer {
            return BaseCameraFileStreamer(
                context,
                logger,
                enableAudio,
                muxer
            ).also { streamer ->
                streamer.onErrorListener = errorListener

                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }

                previewSurface?.let {
                    streamer.startPreview(it)
                }

                file?.let {
                    streamer.file = it
                }
            }
        }
    }
}