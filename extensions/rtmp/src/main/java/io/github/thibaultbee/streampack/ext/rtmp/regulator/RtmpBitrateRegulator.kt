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
import io.github.thibaultbee.streampack.core.regulator.IBitrateRegulator

/**
 * Base class of RTMP bitrate regulation implementation.
 *
 * If you want to implement your custom bitrate regulator, it must inherit from this class.
 * The bitrate regulator object is created by streamers with the [IBitrateRegulator.Factory].
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange call when you have to change audio bitrate
 */
abstract class RtmpBitrateRegulator(
    bitrateRegulatorConfig: BitrateRegulatorConfig,
    onVideoTargetBitrateChange: ((Int) -> Unit),
    onAudioTargetBitrateChange: ((Int) -> Unit)
) : BitrateRegulator<RtmpMetrics>(
    bitrateRegulatorConfig,
    onVideoTargetBitrateChange,
    onAudioTargetBitrateChange
) {
    /**
     * Call regularly to get new RTMP metrics
     *
     * @param metrics RTMP transmission metrics
     * @param currentVideoBitrate current video bitrate target in bits/s.
     * @param currentAudioBitrate current audio bitrate target in bits/s.
     */
    abstract override fun update(
        metrics: RtmpMetrics,
        currentVideoBitrate: Int,
        currentAudioBitrate: Int
    )

    /**
     * Factory interface you must use to create a [RtmpBitrateRegulator] object.
     * If you want to create a custom RTMP bitrate regulation implementation, create a factory that
     * implements this interface.
     */
    interface Factory : IBitrateRegulator.Factory<RtmpMetrics> {
        /**
         * Creates a [RtmpBitrateRegulator] object from given parameters
         *
         * @param bitrateRegulatorConfig bitrate regulation configuration
         * @param onVideoTargetBitrateChange call when you have to change video bitrate
         * @param onAudioTargetBitrateChange call when you have to change audio bitrate
         * @return a [RtmpBitrateRegulator] object
         */
        override fun newBitrateRegulator(
            bitrateRegulatorConfig: BitrateRegulatorConfig,
            onVideoTargetBitrateChange: ((Int) -> Unit),
            onAudioTargetBitrateChange: ((Int) -> Unit)
        ): RtmpBitrateRegulator
    }
}