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
package io.github.thibaultbee.streampack.regulator.controllers

import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.internal.encoders.IPublicEncoder
import io.github.thibaultbee.streampack.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.regulator.IBitrateRegulator
import io.github.thibaultbee.streampack.streamers.interfaces.ICoroutineStreamer

/**
 * The [BitrateRegulatorController] triggers [IBitrateRegulator.update].
 *
 * @param audioEncoder the audio [IPublicEncoder]
 * @param videoEncoder the video [IPublicEncoder]
 * @param endpoint the [IPublicEndpoint] implementation
 * @param bitrateRegulatorFactory the [IBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 */
abstract class BitrateRegulatorController(
    private val audioEncoder: IPublicEncoder?,
    private val videoEncoder: IPublicEncoder?,
    private val endpoint: IPublicEndpoint,
    private val bitrateRegulatorFactory: IBitrateRegulator.Factory,
    private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig()
) : IBitrateRegulatorController {
    abstract class Factory : IBitrateRegulatorController.Factory {
        /**
         * Creates a [IBitrateRegulatorController] object from given parameters
         *
         * @param streamer the [ICoroutineStreamer] implementation.
         * @return a [IBitrateRegulatorController] object
         */
        abstract override fun newBitrateRegulatorController(streamer: ICoroutineStreamer): IBitrateRegulatorController
    }
}
