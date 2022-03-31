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
import io.github.thibaultbee.streampack.internal.sources.AudioCapture
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import java.io.File

/**
 * A [BaseStreamer] that sends only microphone frames to a [File].
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param muxer a [IMuxer] implementation
 */
open class BaseAudioOnlyFileStreamer(
    context: Context,
    logger: ILogger = StreamPackLogger(),
    muxer: IMuxer
) : BaseStreamer(
    context = context,
    logger = logger,
    videoCapture = null,
    audioCapture = AudioCapture(logger),
    manageVideoOrientation = false,
    muxer = muxer,
    endpoint = FileWriter(logger = logger)
), IFileStreamer {
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
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO])
    override fun startStream() = super.startStream()
}