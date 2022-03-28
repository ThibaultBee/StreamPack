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
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerPreviewBuilder
import java.io.File

/**
 * A [BaseCameraFileStreamer] that sends microphone and video frames to a FLV [File].
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 */
class CameraFlvFileStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
) : BaseCameraFileStreamer(
    context = context,
    logger = logger,
    muxer = FlvMuxer(context = context, writeToFile = true),
    enableAudio = enableAudio
) {
    /**
     * Builder class for [BaseCameraFileStreamer] objects. Use this class to configure and create
     * a specific [BaseCameraFileStreamer] instance for FLV.
     */
     class Builder: BaseCameraFileStreamer.Builder() {
        /**
         * Combines all of the characteristics that have been set and return a new
         * [BaseCameraFileStreamer] object specific for FLV.
         *
         * @return a new [BaseCameraFileStreamer] object specific for FLV
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): BaseCameraFileStreamer {
            setMuxerImpl(FlvMuxer(context = context, writeToFile = true))
            return super.build()
        }
    }
}