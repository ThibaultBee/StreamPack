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
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.endpoints.FileWriter
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.sources.AudioCapture
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.logger.StreamPackLogger
import com.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import com.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import java.io.File

/**
 * [BaseCameraStreamer] that sends audio frames to a [File].
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 */
class AudioOnlyTsFileStreamer(
    context: Context,
    tsServiceInfo: ServiceInfo,
    logger: ILogger
) : BaseStreamer(
    context = context,
    tsServiceInfo = tsServiceInfo,
    videoCapture = null,
    audioCapture = AudioCapture(logger),
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

    /**
     * No need for video
     */
    override fun onResetVideo() = false

    /**
     * No need for video
     */
    override suspend fun afterResetVideo() {}

    /**
     * Builder class for [AudioOnlyTsFileStreamer] objects. Use this class to configure and create an [AudioOnlyTsFileStreamer] instance.
     */
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var videoConfig: VideoConfig? = null,
        private var previewSurface: Surface? = null,
        private var file: File? = null,
        private var enableAudio: Boolean = true
    ) : IStreamerBuilder, IFileStreamerBuilder {
        private lateinit var context: Context
        private lateinit var serviceInfo: ServiceInfo

        /**
         * Set application context. It is mandatory to set context.
         *
         * @param context application context.
         */
        override fun setContext(context: Context) = apply { this.context = context }

        /**
         * Set TS service info. It is mandatory to set TS service info.
         *
         * @param serviceInfo TS service info.
         */
        override fun setServiceInfo(serviceInfo: ServiceInfo) =
            apply { this.serviceInfo = serviceInfo }

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
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): AudioOnlyTsFileStreamer {
            return AudioOnlyTsFileStreamer(
                context,
                serviceInfo,
                logger
            )
                .also { streamer ->
                    streamer.configure(audioConfig)
                    streamer.file = file
                }
        }
    }
}