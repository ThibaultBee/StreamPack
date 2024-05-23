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
package io.github.thibaultbee.streampack.ext.srt.regulator.srt

import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.composites.sinks.SrtSink
import io.github.thibaultbee.streampack.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.streamers.interfaces.ICoroutineStreamer
import kotlinx.coroutines.runBlocking

/**
 * SrtStreamerRegulator is a helper class to regulate bitrate of a [ICoroutineStreamer] using SRT.
 *
 * @param streamer the [ICoroutineStreamer] implementation.
 * @param bitrateRegulatorFactory the [IBitrateRegulatorFactory] implementation. Use it to make your own bitrate regulator.
 * @param bitrateRegulatorConfig bitrate regulator configuration
 */
class SrtRegulatorController(
    val streamer: ICoroutineStreamer,
    bitrateRegulatorFactory: IBitrateRegulatorFactory = DefaultSrtBitrateRegulatorFactory(),
    bitrateRegulatorConfig: BitrateRegulatorConfig = BitrateRegulatorConfig()
) {
    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorConfig.let { config ->
        bitrateRegulatorFactory.newBitrateRegulator(
            config,
            {
                val videoEncoder = streamer.videoEncoder
                    ?: throw UnsupportedOperationException("Bitrate regulator set without a video encoder")
                videoEncoder.bitrate = it
            },
            { /* Do nothing for audio */ }
        ) as SrtBitrateRegulator
    }

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = Scheduler(500) {
        val videoEncoder = streamer.videoEncoder
            ?: throw UnsupportedOperationException("Scheduler runs but no video encoder set")
        bitrateRegulator.update(
            srtSink.stats,
            videoEncoder.bitrate,
            streamer.audioEncoder?.bitrate ?: 0
        )
    }

    private val srtSink = (streamer.endpoint as CompositeEndpoint).sink as SrtSink

    init {
        // TODO: use streamer scope
        runBlocking {
            streamer.isStreaming.collect { isStreaming ->
                if (isStreaming) {
                    scheduler.start()
                } else {
                    scheduler.cancel()
                }
            }
        }
    }
}
