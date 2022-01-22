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
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.regulator.DefaultSrtBitrateRegulatorFactory
import com.github.thibaultbee.streampack.streamers.AudioOnlySrtLiveStreamer
import com.github.thibaultbee.streampack.streamers.AudioOnlyTsFileStreamer
import com.github.thibaultbee.streampack.streamers.CameraSrtLiveStreamer
import com.github.thibaultbee.streampack.streamers.CameraTsFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IStreamer

class StreamerFactory(
    private val context: Context,
    private val configuration: Configuration,
) {
    fun build(): IStreamer {
        val tsServiceInfo = ServiceInfo(
            ServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

        val streamerBuilder =
            if (configuration.endpoint.enpointType == Configuration.Endpoint.EndpointType.SRT) {
                if (!configuration.video.enable) {
                    AudioOnlySrtLiveStreamer.Builder()
                } else {
                    CameraSrtLiveStreamer.Builder()
                        .setBitrateRegulator(
                            if (configuration.endpoint.connection.enableBitrateRegulation) {
                                DefaultSrtBitrateRegulatorFactory()
                            } else {
                                null
                            },
                            if (configuration.endpoint.connection.enableBitrateRegulation) {
                                BitrateRegulatorConfig.Builder()
                                    .setVideoBitrateRange(configuration.endpoint.connection.videoBitrateRange)
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
            } else {
                if (!configuration.video.enable) {
                    AudioOnlyTsFileStreamer.Builder()
                } else {
                    CameraTsFileStreamer.Builder()
                }
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
            .setServiceInfo(tsServiceInfo)
            .apply {
                if (!configuration.audio.enable) {
                    disableAudio()
                }
            }
            .setConfiguration(audioConfig, videoConfig)
            .build()
    }
}