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
package io.github.thibaultbee.streampack.streamers.file

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import java.io.File

/**
 * [BaseCameraFileStreamer] that sends microphone and video frames to a TS [File].
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param tsServiceInfo MPEG-TS service description
 */
class CameraTsFileStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
    tsServiceInfo: TsServiceInfo,
) : BaseCameraFileStreamer(
    context = context,
    logger = logger,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    enableAudio = enableAudio
) {
    /**
     * Builder class for [BaseCameraFileStreamer] objects. Use this class to configure and create
     * a specific [BaseCameraFileStreamer] instance for MPEG-TS.
     */
    class Builder : BaseCameraFileStreamer.Builder(), ITsStreamerBuilder {
        private lateinit var tsServiceInfo: TsServiceInfo

        /**
         * Set TS service info. It is mandatory to set TS service info.
         * Mandatory.
         *
         * @param tsServiceInfo TS service info.
         */
        override fun setServiceInfo(tsServiceInfo: TsServiceInfo) =
            apply { this.tsServiceInfo = tsServiceInfo }

        /**
         * Combines all of the characteristics that have been set and return a new
         * [BaseCameraFileStreamer] object specific for MPEG-TS.
         *
         * @return a new [BaseCameraFileStreamer] object specific for MPEG-TS
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): BaseCameraFileStreamer {
            setMuxerImpl(TSMuxer().apply { addService(tsServiceInfo) })
            return super.build()
        }
    }
}