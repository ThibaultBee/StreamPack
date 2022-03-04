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
package com.github.thibaultbee.streampack.streamers.file

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.logger.StreamPackLogger
import com.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import java.io.File

/**
 * [BaseCameraStreamer] that sends audio frames to a TS [File].
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 */
class AudioOnlyTsFileStreamer(
    context: Context,
    tsServiceInfo: TsServiceInfo,
    logger: ILogger
) : BaseAudioOnlyFileStreamer(
    context = context,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    logger = logger
) {
    /**
     * Builder class for [AudioOnlyTsFileStreamer] objects. Use this class to configure and create an [AudioOnlyTsFileStreamer] instance.
     */
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var videoConfig: VideoConfig? = null,
        private var file: File? = null,
    ) : IStreamerBuilder, ITsStreamerBuilder, IFileStreamerBuilder {
        private lateinit var context: Context
        private lateinit var tsServiceInfo: TsServiceInfo

        /**
         * Set application context. It is mandatory to set context.
         *
         * @param context application context.
         */
        override fun setContext(context: Context) = apply { this.context = context }

        /**
         * Set TS service info. It is mandatory to set TS service info.
         *
         * @param tsServiceInfo TS service info.
         */
        override fun setServiceInfo(tsServiceInfo: TsServiceInfo) =
            apply { this.tsServiceInfo = tsServiceInfo }

        /**
         * Set logger.
         *
         * @param logger [ILogger] implementation
         */
        override fun setLogger(logger: ILogger) = apply { this.logger = logger }

        /**
         * Set audio configuration.
         * Configurations can be change later with [configure].
         * Video configuration is not used.
         *
         * @param audioConfig audio configuration
         * @param videoConfig video configuration. Not used.
         */
        override fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig) = apply {
            this.audioConfig = audioConfig
        }

        /**
         * Set audio configurations.
         * Configurations can be change later with [configure].
         *
         * @param audioConfig audio configuration
         */
        override fun setAudioConfiguration(audioConfig: AudioConfig) = apply {
            this.audioConfig = audioConfig
        }

        /**
         * Set video configurations. Do not use.
         *
         * @param videoConfig video configuration
         */
        override fun setVideoConfiguration(videoConfig: VideoConfig) = apply {
            throw UnsupportedOperationException("Do not set video configuration on audio only streamer")
        }

        /**
         * Disable audio. Do not use.
         */
        override fun disableAudio(): IStreamerBuilder {
            throw UnsupportedOperationException("Do not disable audio on audio only streamer")
        }

        /**
         * Set destination file.
         *
         * @param file where to write date
         */
        override fun setFile(file: File) = apply {
            this.file = file
        }

        /**
         * Combines all of the characteristics that have been set and return a new [AudioOnlyTsFileStreamer] object.
         *
         * @return a new [AudioOnlyTsFileStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
        override fun build(): AudioOnlyTsFileStreamer {
            return AudioOnlyTsFileStreamer(
                context,
                tsServiceInfo,
                logger
            )
                .also { streamer ->
                    streamer.configure(audioConfig)
                    streamer.file = file
                }
        }
    }
}