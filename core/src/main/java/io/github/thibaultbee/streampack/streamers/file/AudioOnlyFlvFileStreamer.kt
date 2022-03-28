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
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.logger.ILogger
import java.io.File

/**
 * A [BaseAudioOnlyFileStreamer] that sends only microphone frames to a FLV [File].
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 */
class AudioOnlyFlvFileStreamer(
    context: Context,
    logger: ILogger
) : BaseAudioOnlyFileStreamer(
    context = context,
    logger = logger,
    muxer = FlvMuxer(context = context, writeToFile = true),
) {
    /**
     * Builder class for [BaseAudioOnlyFileStreamer] objects. Use this class to configure and create
     * a specific [BaseAudioOnlyFileStreamer] instance for FLV.
     */
    class Builder : BaseAudioOnlyFileStreamer.Builder() {
        /**
         * Combines all of the characteristics that have been set and return a new
         * [BaseAudioOnlyFileStreamer] object specific for FLV.
         *
         * @return a new [BaseAudioOnlyFileStreamer] object specific for FLV
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
        override fun build(): BaseAudioOnlyFileStreamer {
            setMuxerImpl(FlvMuxer(context = context, writeToFile = true))
            return super.build()
        }
    }
}