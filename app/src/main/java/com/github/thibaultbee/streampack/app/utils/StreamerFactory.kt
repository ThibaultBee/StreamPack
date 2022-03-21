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
package com.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.util.Range
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.app.models.EndpointType
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import com.github.thibaultbee.streampack.regulator.srt.DefaultSrtBitrateRegulatorFactory
import com.github.thibaultbee.streampack.streamers.file.AudioOnlyFlvFileStreamer
import com.github.thibaultbee.streampack.streamers.file.AudioOnlyTsFileStreamer
import com.github.thibaultbee.streampack.streamers.file.CameraFlvFileStreamer
import com.github.thibaultbee.streampack.streamers.file.CameraTsFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IAdaptiveLiveStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import com.github.thibaultbee.streampack.streamers.rtmp.AudioOnlyRtmpLiveStreamer
import com.github.thibaultbee.streampack.streamers.rtmp.CameraRtmpLiveStreamer
import com.github.thibaultbee.streampack.streamers.srt.AudioOnlySrtLiveStreamer
import com.github.thibaultbee.streampack.streamers.srt.CameraSrtLiveStreamer

class StreamerFactory(
    private val context: Context,
    private val configuration: Configuration,
) {
    private fun createTsServiceInfo(): TsServiceInfo {
        return TsServiceInfo(
            TsServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )
    }

    private fun createStreamerBuilder(): IStreamerBuilder {
        return if (configuration.video.enable) {
            when (configuration.endpoint.endpointType) {
                EndpointType.TS_FILE -> CameraTsFileStreamer.Builder()
                EndpointType.FLV_FILE -> CameraFlvFileStreamer.Builder()
                EndpointType.SRT -> CameraSrtLiveStreamer.Builder()
                EndpointType.RTMP -> CameraRtmpLiveStreamer.Builder()
            }
        } else {
            when (configuration.endpoint.endpointType) {
                EndpointType.TS_FILE -> AudioOnlyTsFileStreamer.Builder()
                EndpointType.FLV_FILE -> AudioOnlyFlvFileStreamer.Builder()
                EndpointType.SRT -> AudioOnlySrtLiveStreamer.Builder()
                EndpointType.RTMP -> AudioOnlyRtmpLiveStreamer.Builder()
            }
        }
    }

    fun build(): IStreamer {
        val streamerBuilder = createStreamerBuilder()

        if (streamerBuilder is IAdaptiveLiveStreamerBuilder) {
            streamerBuilder.setBitrateRegulator(
                if (configuration.endpoint.srt.enableBitrateRegulation) {
                    DefaultSrtBitrateRegulatorFactory()
                } else {
                    null
                },
                if (configuration.endpoint.srt.enableBitrateRegulation) {
                    BitrateRegulatorConfig.Builder()
                        .setVideoBitrateRange(configuration.endpoint.srt.videoBitrateRange)
                        .setAudioBitrateRange(
                            Range(
                                configuration.audio.bitrate,
                                configuration.audio.bitrate
                            )
                        )
                        .build()
                } else {
                    null
                },
            )
        }

        if (streamerBuilder is ITsStreamerBuilder) {
            streamerBuilder.setServiceInfo(createTsServiceInfo())
        }

        if (!configuration.audio.enable) {
            streamerBuilder.disableAudio()
        }

        val videoConfig = VideoConfig.Builder()
            .setMimeType(configuration.video.encoder)
            .setStartBitrate(configuration.video.bitrate * 1000)  // to b/s
            .setResolution(configuration.video.resolution)
            .setFps(configuration.video.fps)
            .build()

        val audioConfig = AudioConfig.Builder()
            .setMimeType(configuration.audio.encoder)
            .setStartBitrate(configuration.audio.bitrate)
            .setSampleRate(configuration.audio.sampleRate)
            .setNumberOfChannel(configuration.audio.numberOfChannels)
            .setByteFormat(configuration.audio.byteFormat)
            .setEchoCanceler(configuration.audio.enableEchoCanceler)
            .setNoiseSuppressor(configuration.audio.enableNoiseSuppressor)
            .build()


        return streamerBuilder
            .setContext(context)
            .setConfiguration(audioConfig, videoConfig)
            .build()
    }
}