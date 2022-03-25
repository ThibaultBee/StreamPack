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
package io.github.thibaultbee.streampack.ext.srt.regulator.srt

import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory

/**
 * Factory that creates [DefaultSrtBitrateRegulator].
 */
class DefaultSrtBitrateRegulatorFactory : IBitrateRegulatorFactory {

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
    ) = DefaultSrtBitrateRegulator(
        bitrateRegulatorConfig,
        onVideoTargetBitrateChange,
        onAudioTargetBitrateChange
    )
}