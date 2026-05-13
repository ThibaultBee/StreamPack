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
package io.github.thibaultbee.streampack.core.regulator.controllers

import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.interfaces.WithMetrics
import io.github.thibaultbee.streampack.core.elements.utils.CoroutineScheduler
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.regulator.IBitrateRegulator
import kotlinx.coroutines.CoroutineDispatcher

/**
 * A [BitrateRegulatorController] implementation that triggers [IBitrateRegulator.update] every [pollingTimeInMs].
 *
 * @param audioEncoder the audio [IEncoder]
 * @param videoEncoder the video [IEncoder]
 * @param metricsProvider the [WithMetrics] implementation
 * @param bitrateRegulatorFactory the [IBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 * @param pollingTimeInMs delay between each call to [IBitrateRegulator.update]
 */
open class IntervalBitrateRegulatorController<T : Any>(
    audioEncoder: IEncoder?,
    videoEncoder: IEncoder,
    metricsProvider: WithMetrics<T>,
    bitrateRegulatorFactory: IBitrateRegulator.Factory<T>,
    coroutineDispatcher: CoroutineDispatcher,
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
    pollingTimeInMs: Long = DEFAULT_POLLING_TIME_IN_MS
) : BitrateRegulatorController(
    audioEncoder,
    videoEncoder,
    metricsProvider,
    bitrateRegulatorFactory,
    bitrateRegulatorConfig
) {
    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorFactory.newBitrateRegulator(
        bitrateRegulatorConfig,
        {
            videoEncoder.bitrate = it
        },
        { /* Do nothing for audio */ }
    )

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = CoroutineScheduler(pollingTimeInMs, coroutineDispatcher) {
        bitrateRegulator.update(
            metricsProvider.metrics,
            videoEncoder.bitrate,
            audioEncoder?.bitrate ?: 0
        )
    }

    override fun start() {
        scheduler.start()
    }

    override fun stop() {
        scheduler.stop()
    }

    companion object {
        const val DEFAULT_POLLING_TIME_IN_MS = 500L
    }

    class Factory<T : Any>(
        private val bitrateRegulatorFactory: IBitrateRegulator.Factory<T>,
        private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
        private val pollingTimeInMs: Long = DEFAULT_POLLING_TIME_IN_MS
    ) : BitrateRegulatorController.Factory() {
        override fun newBitrateRegulatorController(
            pipelineOutput: IEncodingPipelineOutput,
            coroutineDispatcher: CoroutineDispatcher
        ): BitrateRegulatorController {
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
            @Suppress("UNCHECKED_CAST")
            val endpoint = pipelineOutput.endpoint as WithMetrics<T>
            return IntervalBitrateRegulatorController(
                audioEncoder,
                videoEncoder,
                endpoint,
                bitrateRegulatorFactory,
                coroutineDispatcher,
                bitrateRegulatorConfig,
                pollingTimeInMs
            )
        }
    }
}
