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
package com.github.thibaultbee.streampack.regulator

import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig


/**
 * Factory interface you must use to create a [SrtBitrateRegulator] object.
 * If you want to create a custom bitrate regulation implementation, create a factory that
 * implements this interface.
 */
interface ISrtBitrateRegulatorFactory {

    /**
     * Creates a [SrtBitrateRegulator] object from given parameters
     *
     * @param bitrateRegulatorConfig bitrate regulation configuration
     * @param onVideoTargetBitrateChange call when you have to change video bitrate
     * @param onAudioTargetBitrateChange call when you have to change audio bitrate
     * @return a [SrtBitrateRegulator] object
     */
    fun newSrtBitrateRegulator(
        bitrateRegulatorConfig: BitrateRegulatorConfig,
        onVideoTargetBitrateChange: ((Int) -> Unit),
        onAudioTargetBitrateChange: ((Int) -> Unit)
    ): SrtBitrateRegulator
}