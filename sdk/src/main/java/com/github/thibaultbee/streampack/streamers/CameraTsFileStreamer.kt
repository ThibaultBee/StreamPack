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
import com.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IFileStreamerBuilder
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.logger.StreamPackLogger
import java.io.File

/**
 * [BaseCameraStreamer] that sends audio/video frames to a [File].
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 */
class CameraTsFileStreamer(
    context: Context,
    tsServiceInfo: ServiceInfo,
    logger: ILogger
) : BaseCameraStreamer(context, tsServiceInfo, FileWriter(logger), logger), IFileStreamer {
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

    /**
     * Builder class for [CameraTsFileStreamer] objects. Use this class to configure and create an [CameraTsFileStreamer] instance.
     */
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var videoConfig: VideoConfig? = null,
        private var previewSurface: Surface? = null,
        private var file: File? = null,
    ) : IFileStreamerBuilder {
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
         * Set both audio and video configuration. Can be change with [configure].
         *
         * @param audioConfig audio configuration
         * @param videoConfig video configuration
         */
        override fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig) = apply {
            this.audioConfig = audioConfig
            this.videoConfig = videoConfig
        }

        /**
         * Set preview surface.
         * If provided, it starts preview.
         *
         * @param previewSurface surface where to display preview
         */
        override fun setPreviewSurface(previewSurface: Surface) = apply {
            this.previewSurface = previewSurface
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
         * Combines all of the characteristics that have been set and return a new [CameraTsFileStreamer] object.
         *
         * @return a new [CameraTsFileStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): CameraTsFileStreamer {
            val streamer = CameraTsFileStreamer(context, serviceInfo, logger)

            if ((audioConfig != null) && (videoConfig != null)) {
                streamer.configure(audioConfig!!, videoConfig!!)
            }

            previewSurface?.let {
                streamer.startPreview(it)
            }

            file?.let {
                streamer.file = it
            }

            return streamer
        }
    }
}