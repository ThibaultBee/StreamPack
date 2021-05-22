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
package com.github.thibaultbee.streampack.streamers

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.internal.endpoints.FileWriter
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.ILogger
import java.io.File

/**
 * [BaseCaptureStreamer] that sends audio/video frames to a [File].
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 */
class CaptureFileStreamer(
    context: Context,
    tsServiceInfo: ServiceInfo,
    val logger: ILogger
) : BaseCaptureStreamer(context, tsServiceInfo, FileWriter(logger), logger) {
    private val fileWriter = endpoint as FileWriter

    /**
     * Get/Set [FileWriter] file. If no file has been set. [FileWriter] uses a default temporary file.
     */
    var file: File
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
     * Same as [BaseCaptureStreamer.startStream] with RequiresPermission annotation for
     * Manifest.permission.WRITE_EXTERNAL_STORAGE.
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun startStream() = super.startStream()
}