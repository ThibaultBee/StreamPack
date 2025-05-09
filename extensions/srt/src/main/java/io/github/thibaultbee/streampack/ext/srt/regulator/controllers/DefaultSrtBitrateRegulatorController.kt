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
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.regulator.controllers.BitrateRegulatorController
import io.github.thibaultbee.streampack.core.regulator.controllers.DummyBitrateRegulatorController
import io.github.thibaultbee.streampack.ext.srt.regulator.DefaultSrtBitrateRegulator
import io.github.thibaultbee.streampack.ext.srt.regulator.SrtBitrateRegulator

/**
 * A [DummyBitrateRegulatorController] implementation for a [SrtSink].
 */
class DefaultSrtBitrateRegulatorController {
    class Factory(
        private val bitrateRegulatorFactory: SrtBitrateRegulator.Factory = DefaultSrtBitrateRegulator.Factory(),
        private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
        private val delayTimeInMs: Long = 500
    ) : BitrateRegulatorController.Factory() {
        override fun newBitrateRegulatorController(pipelineOutput: IEncodingPipelineOutput): DummyBitrateRegulatorController {
            require(pipelineOutput is IConfigurableVideoEncodingPipelineOutput) {
                "Pipeline output must be an video encoding output"
            }

            val videoEncoder = requireNotNull(pipelineOutput.videoEncoder) {
                "Video encoder must be set"
            }

            val audioEncoder = if (pipelineOutput is IConfigurableAudioEncodingPipelineOutput) {
                pipelineOutput.audioEncoder
            } else {
                null
            }
            return DummyBitrateRegulatorController(
                audioEncoder,
                videoEncoder,
                pipelineOutput.endpoint,
                bitrateRegulatorFactory,
                bitrateRegulatorConfig,
                delayTimeInMs
            )
        }
    }
}
