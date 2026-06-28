/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.ext.srt.regulator.controllers

import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.regulator.IBitrateRegulator
import io.github.thibaultbee.streampack.core.regulator.controllers.IntervalBitrateRegulatorController
import io.github.thibaultbee.streampack.core.regulator.controllers.IntervalBitrateRegulatorController.Companion.DEFAULT_POLLING_TIME
import io.github.thibaultbee.streampack.ext.srt.regulator.DummySrtBitrateRegulator
import io.github.thibaultbee.streampack.ext.srt.regulator.SrtBitrateRegulator
import kotlin.time.Duration

/**
 * A [IntervalBitrateRegulatorController.Factory] for [SrtBitrateRegulator].
 *
 * @param bitrateRegulatorFactory the [SrtBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 * @param pollingTime delay between each call to [IBitrateRegulator.update]
 *
 * @see IntervalBitrateRegulatorController.Factory
 * @see DummySrtBitrateRegulator.Factory
 */
fun intervalSrtBitrateRegulatorControllerFactory(
    bitrateRegulatorFactory: SrtBitrateRegulator.Factory = DummySrtBitrateRegulator.Factory(),
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
    pollingTime: Duration = DEFAULT_POLLING_TIME
) = IntervalBitrateRegulatorController.Factory(
    bitrateRegulatorFactory,
    bitrateRegulatorConfig,
    pollingTime
)
