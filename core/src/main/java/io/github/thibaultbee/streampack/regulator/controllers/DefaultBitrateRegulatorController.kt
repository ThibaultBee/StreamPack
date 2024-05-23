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
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.regulator.IBitrateRegulator
import io.github.thibaultbee.streampack.streamers.interfaces.ICoroutineStreamer

/**
 * A [BitrateRegulatorController] implementation that triggers [IBitrateRegulator.update] every [delayTimeInMs].
 *
 * @param audioEncoder the audio [IPublicEncoder]
 * @param videoEncoder the video [IPublicEncoder]
 * @param endpoint the [IPublicEndpoint] implementation
 * @param bitrateRegulatorFactory the [IBitrateRegulator.Factory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 * @param delayTimeInMs delay between each call to [IBitrateRegulator.update]
 */
open class DefaultBitrateRegulatorController(
    audioEncoder: IPublicEncoder?,
    videoEncoder: IPublicEncoder?,
    endpoint: IPublicEndpoint,
    bitrateRegulatorFactory: IBitrateRegulator.Factory,
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
    delayTimeInMs: Long = 500
) : BitrateRegulatorController(
    audioEncoder,
    videoEncoder,
    endpoint,
    bitrateRegulatorFactory,
    bitrateRegulatorConfig
) {
    init {
        require(videoEncoder != null) { "Video encoder is required" }
    }

    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorFactory.newBitrateRegulator(
        bitrateRegulatorConfig,
        {
            Logger.i("TEST>>>", "Video target bitrate changed to $it")
            videoEncoder!!.bitrate = it
        },
        { /* Do nothing for audio */ }
    )

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = Scheduler(delayTimeInMs) {
        bitrateRegulator.update(
            endpoint.metrics,
            videoEncoder?.bitrate ?: 0,
            audioEncoder?.bitrate ?: 0
        )
    }

    override fun start() {
        scheduler.start()
    }

    override fun stop() {
        scheduler.stop()
    }

    class Factory(
        private val bitrateRegulatorFactory: IBitrateRegulator.Factory,
        private val bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig(),
        private val delayTimeInMs: Long = 500
    ) : BitrateRegulatorController.Factory() {
        override fun newBitrateRegulatorController(streamer: ICoroutineStreamer): BitrateRegulatorController {
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
