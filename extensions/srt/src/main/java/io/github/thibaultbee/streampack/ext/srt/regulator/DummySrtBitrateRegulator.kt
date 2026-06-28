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

import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetricsTracker
import io.github.thibaultbee.streampack.ext.srt.utils.SrtEndpointMetrics
import kotlin.math.max
import kotlin.math.min

/**
 * An example of bitrate regulation implementation without memory.
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange not used in this implementation.
 */
class DummySrtBitrateRegulator(
    metricsTracker: EndpointMetricsTracker,
    bitrateRegulatorConfig: BitrateRegulatorConfig,
    onVideoTargetBitrateChange: ((Int) -> Unit),
    onAudioTargetBitrateChange: ((Int) -> Unit)
) : SrtBitrateRegulator(
    metricsTracker,
    bitrateRegulatorConfig,
    onVideoTargetBitrateChange,
    onAudioTargetBitrateChange
) {
    companion object {
        const val MINIMUM_DECREASE_THRESHOLD = 100000 // b/s
        const val MAXIMUM_INCREASE_THRESHOLD = 200000 // b/s
        const val SEND_PACKET_THRESHOLD = 50
    }

    override fun update(currentVideoBitrate: Int, currentAudioBitrate: Int) {
        val metrics = metricsTracker.cumulative as SrtEndpointMetrics
        val stats = metrics.rawMetrics.bistatsOrNull(clear = true, instantaneous = true) ?: return
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
                onVideoTargetBitrateChange(newVideoBitrate)
                return
            }
            // Can bitrate go upper?
        } else if (currentVideoBitrate < bitrateRegulatorConfig.videoBitrateRange.upper) {
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
                onVideoTargetBitrateChange(newVideoBitrate)
                return
            }
        }
    }

    /**
     * Factory that creates a [DummySrtBitrateRegulator].
     */
    class Factory : SrtBitrateRegulator.Factory {

        /**
         * Creates a [DummySrtBitrateRegulator] object from given parameters
         *
         * @param metricsTracker endpoint metrics tracker
         * @param bitrateRegulatorConfig bitrate regulation configuration
         * @param onVideoTargetBitrateChange call when you have to change video bitrate
         * @param onAudioTargetBitrateChange call when you have to change audio bitrate
         * @return a [DummySrtBitrateRegulator] object
         */
        override fun newBitrateRegulator(
            metricsTracker: EndpointMetricsTracker,
            bitrateRegulatorConfig: BitrateRegulatorConfig,
            onVideoTargetBitrateChange: ((Int) -> Unit),
            onAudioTargetBitrateChange: ((Int) -> Unit)
        ): DummySrtBitrateRegulator {
            return DummySrtBitrateRegulator(
                metricsTracker,
                bitrateRegulatorConfig,
                onVideoTargetBitrateChange,
                onAudioTargetBitrateChange
            )
        }
    }
}
