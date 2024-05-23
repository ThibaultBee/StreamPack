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

import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.regulator.DefaultSrtBitrateRegulator
import io.github.thibaultbee.streampack.ext.srt.regulator.SrtBitrateRegulator
import io.github.thibaultbee.streampack.regulator.controllers.BitrateRegulatorController
import io.github.thibaultbee.streampack.regulator.controllers.DefaultBitrateRegulatorController
import io.github.thibaultbee.streampack.streamers.interfaces.ICoroutineStreamer

/**
 * A [DefaultBitrateRegulatorController] implementation for a [SrtSink].
 */
class DefaultSrtBitrateRegulatorController {
    class Factory(
        private val bitrateRegulatorFactory: SrtBitrateRegulator.Factory = DefaultSrtBitrateRegulator.Factory(),
        private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
        private val delayTimeInMs: Long = 500
    ) : BitrateRegulatorController.Factory() {
        override fun newBitrateRegulatorController(streamer: ICoroutineStreamer): DefaultBitrateRegulatorController {
            return DefaultBitrateRegulatorController(
                streamer.audioEncoder,
                streamer.videoEncoder,
                streamer.endpoint,
                bitrateRegulatorFactory,
                bitrateRegulatorConfig,
                delayTimeInMs
            )
        }

    }
}
