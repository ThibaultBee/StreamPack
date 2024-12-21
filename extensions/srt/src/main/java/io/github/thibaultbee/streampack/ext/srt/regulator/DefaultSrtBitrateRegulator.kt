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
package io.github.thibaultbee.streampack.ext.srt.regulator

import io.github.thibaultbee.srtdroid.core.models.Stats
import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import kotlin.math.max
import kotlin.math.min

/**
 * An example of bitrate regulation implementation without memory.
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange not used in this implementation.
 */
class DefaultSrtBitrateRegulator(
    bitrateRegulatorConfig: BitrateRegulatorConfig,
    onVideoTargetBitrateChange: ((Int) -> Unit),
    onAudioTargetBitrateChange: ((Int) -> Unit)
) : SrtBitrateRegulator(
    bitrateRegulatorConfig,
    onVideoTargetBitrateChange,
    onAudioTargetBitrateChange
) {
    companion object {
        const val MINIMUM_DECREASE_THRESHOLD = 100000 // b/s
        const val MAXIMUM_INCREASE_THRESHOLD = 200000 // b/s
        const val SEND_PACKET_THRESHOLD = 50
    }

    override fun update(stats: Stats, currentVideoBitrate: Int, currentAudioBitrate: Int) {
        val estimatedBandwidth = (stats.mbpsBandwidth * 1000000).toInt()

        if (currentVideoBitrate > bitrateRegulatorConfig.videoBitrateRange.lower) {
            val newVideoBitrate = when {
                stats.pktSndLoss > 0 -> {
                    // Detected packet loss - quickly react
                    currentVideoBitrate - max(
                        currentVideoBitrate * 20 / 100, // too late - drop bitrate by 20 %
                        MINIMUM_DECREASE_THRESHOLD // getting down by 100000 b/s minimum
                    )
                }

                stats.pktSndBuf > SEND_PACKET_THRESHOLD -> {
                    // Try to avoid congestion
                    currentVideoBitrate - max(
                        currentVideoBitrate * 10 / 100, // drop bitrate by 10 %
                        MINIMUM_DECREASE_THRESHOLD // getting down by 100000 b/s minimum
                    )
                }

                (currentVideoBitrate + currentAudioBitrate) > estimatedBandwidth -> {
                    // Estimated bitrate too low
                    estimatedBandwidth - currentAudioBitrate
                }

                else -> 0
            }

            if (newVideoBitrate != 0) {
                onVideoTargetBitrateChange(
                    max(
                        newVideoBitrate,
                        bitrateRegulatorConfig.videoBitrateRange.lower
                    )
                ) // Don't go under videoBitrateRange.lower
                return
            }
        }

        // Can bitrate go upper?
        if (currentVideoBitrate < bitrateRegulatorConfig.videoBitrateRange.upper) {
            val newVideoBitrate = when {
                (currentVideoBitrate + currentAudioBitrate) < estimatedBandwidth -> {
                    currentVideoBitrate + min(
                        (bitrateRegulatorConfig.videoBitrateRange.upper - currentVideoBitrate) * 50 / 100, // getting slower when reaching target bitrate
                        MAXIMUM_INCREASE_THRESHOLD // not increasing to fast
                    )
                }

                else -> 0
            }

            if (newVideoBitrate != 0) {
                onVideoTargetBitrateChange(
                    max(
                        newVideoBitrate,
                        bitrateRegulatorConfig.videoBitrateRange.lower
                    )
                ) // Don't go under videoBitrateRange.lower
                return
            }
        }
    }

    /**
     * Factory that creates a [DefaultSrtBitrateRegulator].
     */
    class Factory : SrtBitrateRegulator.Factory {

        /**
         * Creates a [DefaultSrtBitrateRegulator] object from given parameters
         *
         * @param bitrateRegulatorConfig bitrate regulation configuration
         * @param onVideoTargetBitrateChange call when you have to change video bitrate
         * @param onAudioTargetBitrateChange call when you have to change audio bitrate
         * @return a [DefaultSrtBitrateRegulator] object
         */
        override fun newBitrateRegulator(
            bitrateRegulatorConfig: BitrateRegulatorConfig,
            onVideoTargetBitrateChange: ((Int) -> Unit),
            onAudioTargetBitrateChange: ((Int) -> Unit)
        ): DefaultSrtBitrateRegulator {
            return DefaultSrtBitrateRegulator(
                bitrateRegulatorConfig,
                onVideoTargetBitrateChange,
                onAudioTargetBitrateChange
            )
        }
    }
}
