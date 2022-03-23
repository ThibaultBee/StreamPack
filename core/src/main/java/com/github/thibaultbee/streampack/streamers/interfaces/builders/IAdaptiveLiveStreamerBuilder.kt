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
package com.github.thibaultbee.streampack.streamers.interfaces.builders

import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import com.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory

interface IAdaptiveLiveStreamerBuilder : ILiveStreamerBuilder {
    /**
     * Set SRT bitrate regulator class and configuration.
     *
     * @param bitrateRegulatorFactory bitrate regulator factory. If you don't want to implement your own bitrate regulator, use [IBitrateRegulatorFactory]
     * @param bitrateRegulatorConfig bitrate regulator configuration.
     */
    fun setBitrateRegulator(
        bitrateRegulatorFactory: IBitrateRegulatorFactory?,
        bitrateRegulatorConfig: BitrateRegulatorConfig?
    ): IStreamerBuilder
}