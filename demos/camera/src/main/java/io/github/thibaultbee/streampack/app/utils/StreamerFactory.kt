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
package io.github.thibaultbee.streampack.app.utils

import android.Manifest
import android.content.Context
import android.util.Range
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.app.configuration.Configuration
import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.rtmp.streamers.AudioOnlyRtmpLiveStreamer
import io.github.thibaultbee.streampack.ext.rtmp.streamers.CameraRtmpLiveStreamer
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.DefaultSrtBitrateRegulatorFactory
import io.github.thibaultbee.streampack.ext.srt.streamers.AudioOnlySrtLiveStreamer
import io.github.thibaultbee.streampack.ext.srt.streamers.CameraSrtLiveStreamer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.streamers.file.AudioOnlyFlvFileStreamer
import io.github.thibaultbee.streampack.streamers.file.AudioOnlyTsFileStreamer
import io.github.thibaultbee.streampack.streamers.file.CameraFlvFileStreamer
import io.github.thibaultbee.streampack.streamers.file.CameraTsFileStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IStreamer

class StreamerFactory(
    private val context: Context,
    private val configuration: Configuration,
) {
    private val enableAudio: Boolean
        get() = configuration.audio.enable

    private val tsServiceInfo: TsServiceInfo
        get() = TsServiceInfo(
            TsServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

    private val bitrateRegulatorFactory: IBitrateRegulatorFactory?
        get() = if (configuration.endpoint.srt.enableBitrateRegulation) {
            DefaultSrtBitrateRegulatorFactory()
        } else {
            null
        }

    private val bitrateRegulatorConfig: BitrateRegulatorConfig?
        get() = if (configuration.endpoint.srt.enableBitrateRegulation) {
            BitrateRegulatorConfig(
                configuration.endpoint.srt.videoBitrateRange,
                Range(configuration.audio.bitrate, configuration.audio.bitrate)
            )
        } else {
            null
        }


    private fun createStreamer(context: Context): IStreamer {
        return when {
            configuration.video.enable -> {
                when (configuration.endpoint.endpointType) {
                    EndpointType.TS_FILE -> CameraTsFileStreamer(
                        context,
                        enableAudio = enableAudio,
                        tsServiceInfo = tsServiceInfo
                    )
                    EndpointType.FLV_FILE -> CameraFlvFileStreamer(
                        context,
                        enableAudio = enableAudio
                    )
                    EndpointType.SRT -> CameraSrtLiveStreamer(
                        context,
                        enableAudio = enableAudio,
                        tsServiceInfo = tsServiceInfo,
                        bitrateRegulatorFactory = bitrateRegulatorFactory,
                        bitrateRegulatorConfig = bitrateRegulatorConfig
                    )
                    EndpointType.RTMP -> CameraRtmpLiveStreamer(
                        context,
                        enableAudio = enableAudio
                    )
                }
            }
            configuration.audio.enable -> {
                when (configuration.endpoint.endpointType) {
                    EndpointType.TS_FILE -> AudioOnlyTsFileStreamer(
                        context,
                        tsServiceInfo = tsServiceInfo
                    )
                    EndpointType.FLV_FILE -> AudioOnlyFlvFileStreamer(context)
                    EndpointType.SRT -> AudioOnlySrtLiveStreamer(
                        context,
                        tsServiceInfo = tsServiceInfo
                    )
                    EndpointType.RTMP -> AudioOnlyRtmpLiveStreamer(context)
                }
            }
            else -> {
                throw IllegalStateException("StreamerFactory: You must enable at least one of audio or video")
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun build(): IStreamer {
        val streamer = createStreamer(context)

        val videoConfig = VideoConfig(
            mimeType = configuration.video.encoder,
            startBitrate = configuration.video.bitrate * 1000, // to b/s
            resolution = configuration.video.resolution,
            fps = configuration.video.fps
        )

        val audioConfig = AudioConfig(
            mimeType = configuration.audio.encoder,
            startBitrate = configuration.audio.bitrate,
            sampleRate = configuration.audio.sampleRate,
            channelConfig = AudioConfig.getChannelConfig(configuration.audio.numberOfChannels),
            byteFormat = configuration.audio.byteFormat,
            enableEchoCanceler = configuration.audio.enableEchoCanceler,
            enableNoiseSuppressor = configuration.audio.enableNoiseSuppressor
        )
        if (configuration.video.enable) {
            streamer.configure(videoConfig)
        }

        if (configuration.audio.enable) {
            streamer.configure(audioConfig)
        }

        return streamer
    }
}