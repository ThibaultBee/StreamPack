/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.regulator

import io.github.komedia.komuxer.rtmp.util.metrics.RtmpMetrics
import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.regulator.BitrateRegulator
import kotlin.math.max
import kotlin.math.min

/**
 * A [BitrateRegulator] that reduce video bitrate when packets are lost.
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange call when you have to change audio bitrate
 */
class SimpleRtmpBitrateRegulator(
    bitrateRegulatorConfig: BitrateRegulatorConfig,
    onVideoTargetBitrateChange: ((Int) -> Unit),
    onAudioTargetBitrateChange: ((Int) -> Unit)
) : RtmpBitrateRegulator(
    bitrateRegulatorConfig,
    onVideoTargetBitrateChange,
    onAudioTargetBitrateChange
) {
    companion object {
        const val MINIMUM_DECREASE_THRESHOLD = 100000 // b/s
        const val MAXIMUM_INCREASE_THRESHOLD = 200000 // b/s
    }

    /**
     * Call regularly to get new RTMP metrics
     *
     * @param metrics RTMP transmission metrics
     * @param currentVideoBitrate current video bitrate target in bits/s.
     * @param currentAudioBitrate current audio bitrate target in bits/s.
     */
    override fun update(metrics: RtmpMetrics, currentVideoBitrate: Int, currentAudioBitrate: Int) {
        if (metrics.messagesSendDropped > 0) {
            // Detected packet loss - quickly react
            val newVideoBitrate = currentVideoBitrate - max(
                currentVideoBitrate * 20 / 100, // too late - drop bitrate by 20 %
                MINIMUM_DECREASE_THRESHOLD // getting down by 100000 b/s minimum
            )
            onVideoTargetBitrateChange(
                max(
                    newVideoBitrate,
                    bitrateRegulatorConfig.videoBitrateRange.lower
                )
            )
        } else if (currentVideoBitrate < bitrateRegulatorConfig.videoBitrateRange.upper) {
            // Try to increase to the max target
            val newVideoBitrate = currentVideoBitrate + min(
                (bitrateRegulatorConfig.videoBitrateRange.upper - currentVideoBitrate) * 50 / 100, // getting slower when reaching target bitrate
                MAXIMUM_INCREASE_THRESHOLD // not increasing to fast
            )
            onVideoTargetBitrateChange(
                min(
                    newVideoBitrate,
                    bitrateRegulatorConfig.videoBitrateRange.upper
                )
            )
        }
    }

    /**
     * Factory interface you must use to create a [SimpleRtmpBitrateRegulator] object.
     * If you want to create a custom RTMP bitrate regulation implementation, create a factory that
     * implements this interface.
     */
    class Factory : RtmpBitrateRegulator.Factory {
        /**
         * Creates a [SimpleRtmpBitrateRegulator] object from given parameters
         *
         * @param bitrateRegulatorConfig bitrate regulation configuration
         * @param onVideoTargetBitrateChange call when you have to change video bitrate
         * @param onAudioTargetBitrateChange call when you have to change audio bitrate
         * @return a [SimpleRtmpBitrateRegulator] object
         */
        override fun newBitrateRegulator(
            bitrateRegulatorConfig: BitrateRegulatorConfig,
            onVideoTargetBitrateChange: ((Int) -> Unit),
            onAudioTargetBitrateChange: ((Int) -> Unit)
        ): SimpleRtmpBitrateRegulator {
            return SimpleRtmpBitrateRegulator(
                bitrateRegulatorConfig,
                onVideoTargetBitrateChange,
                onAudioTargetBitrateChange
            )
        }
    }
}