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
import io.github.thibaultbee.streampack.core.elements.metrics.WithEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.createMetricsTracker
import io.github.thibaultbee.streampack.core.elements.utils.CoroutineScheduler
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.regulator.IBitrateRegulator
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [BitrateRegulatorController] implementation that triggers [IBitrateRegulator.update] every [pollingTime].
 *
 * @param audioEncoder the audio [IEncoder]
 * @param videoEncoder the video [IEncoder]
 * @param metricsProvider the [WithEndpointMetrics] implementation
 * @param bitrateRegulatorFactory the [IBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 * @param pollingTime delay between each call to [IBitrateRegulator.update]
 */
class IntervalBitrateRegulatorController(
    audioEncoder: IEncoder?,
    videoEncoder: IEncoder,
    metricsProvider: WithEndpointMetrics<*>,
    bitrateRegulatorFactory: IBitrateRegulator.Factory,
    coroutineDispatcher: CoroutineDispatcher,
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
    pollingTime: Duration = DEFAULT_POLLING_TIME
) : BitrateRegulatorController(
    audioEncoder,
    videoEncoder,
    metricsProvider,
    bitrateRegulatorFactory,
    bitrateRegulatorConfig
) {
    private val metricsTracker = metricsProvider.createMetricsTracker()

    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorFactory.newBitrateRegulator(
        metricsTracker,
        bitrateRegulatorConfig,
        onVideoTargetBitrateChange = {
            videoEncoder.bitrate = it
        },
        onAudioTargetBitrateChange = { /* Do nothing for audio */ }
    )

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = CoroutineScheduler(pollingTime, coroutineDispatcher) {
        bitrateRegulator.update(
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
        val DEFAULT_POLLING_TIME = 500.milliseconds
    }

    class Factory(
        private val bitrateRegulatorFactory: IBitrateRegulator.Factory,
        private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
        private val pollingTime: Duration = DEFAULT_POLLING_TIME
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
            val endpoint = pipelineOutput.endpoint as WithEndpointMetrics<*>
            return IntervalBitrateRegulatorController(
                audioEncoder,
                videoEncoder,
                endpoint,
                bitrateRegulatorFactory,
                coroutineDispatcher,
                bitrateRegulatorConfig,
                pollingTime
            )
        }
    }
}
