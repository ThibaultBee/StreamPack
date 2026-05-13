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
package io.github.thibaultbee.streampack.ext.rtmp.regulator.controllers

import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.regulator.controllers.IntervalBitrateRegulatorController
import io.github.thibaultbee.streampack.core.regulator.controllers.IntervalBitrateRegulatorController.Companion.DEFAULT_POLLING_TIME_IN_MS
import io.github.thibaultbee.streampack.ext.rtmp.regulator.RtmpBitrateRegulator
import io.github.thibaultbee.streampack.ext.rtmp.regulator.SimpleRtmpBitrateRegulator

/**
 * A [IntervalBitrateRegulatorController.Factory] for [RtmpBitrateRegulator].
 *
 * @param bitrateRegulatorFactory the [RtmpBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 * @param pollingTimeInMs delay between each call to [RtmpBitrateRegulator.update]
 *
 * @see IntervalBitrateRegulatorController.Factory
 * @see SimpleRtmpBitrateRegulator.Factory
 */
fun intervalRtmpBitrateRegulatorControllerFactory(
    bitrateRegulatorFactory: RtmpBitrateRegulator.Factory = SimpleRtmpBitrateRegulator.Factory(),
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
    pollingTimeInMs: Long = DEFAULT_POLLING_TIME_IN_MS
) = IntervalBitrateRegulatorController.Factory(
    bitrateRegulatorFactory, bitrateRegulatorConfig, pollingTimeInMs
)
