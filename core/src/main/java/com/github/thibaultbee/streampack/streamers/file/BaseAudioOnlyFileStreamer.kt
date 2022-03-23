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
package com.github.thibaultbee.streampack.streamers.file

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.endpoints.FileWriter
import com.github.thibaultbee.streampack.internal.muxers.IMuxer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import com.github.thibaultbee.streampack.internal.sources.AudioCapture
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.logger.StreamPackLogger
import com.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import com.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import java.io.File

/**
 * [BaseStreamer] that sends audio frames to a [File].
 *
 * @param context application context
 * @param muxer a [IMuxer] implementation
 * @param logger a [ILogger] implementation
 */
open class BaseAudioOnlyFileStreamer(
    context: Context,
    muxer: IMuxer,
    logger: ILogger
) : BaseStreamer(
    context = context,
    videoCapture = null,
    audioCapture = AudioCapture(logger),
    muxer = muxer,
    endpoint = FileWriter(logger = logger),
    logger = logger
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