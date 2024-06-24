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
package io.github.thibaultbee.streampack.core.internal.endpoints

import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.core.internal.interfaces.SuspendCloseable
import io.github.thibaultbee.streampack.core.internal.interfaces.SuspendStreamable
import kotlinx.coroutines.flow.StateFlow


interface IEndpoint : IPublicEndpoint, SuspendStreamable,
    SuspendCloseable, Releaseable {
    /**
     * Opens the endpoint.
     * The endpoint must check if the [MediaDescriptor] is supported and if it is not already opened.
     * @param descriptor the media descriptor
     */
    suspend fun open(descriptor: MediaDescriptor)

    /**
     * Writes a [Frame] to the [IEndpoint].
     *
     * @param frame the [Frame] to write
     * @param streamPid the stream id the [Frame] belongs to
     */
    suspend fun write(
        frame: Frame,
        streamPid: Int
    )

    /**
     * Registers new streams to the [IEndpoint].
     *
     * @param streamConfigs the list of [Config] to register
     * @return the map of [Config] to their corresponding stream id
     */
    fun addStreams(streamConfigs: List<Config>): Map<Config, Int>

    /**
     * Registers a new stream to the [IEndpoint].
     *
     * @param streamConfig the [Config] to register
     * @return the stream id
     */
    fun addStream(streamConfig: Config): Int
}

interface IPublicEndpoint {
    /**
     * Whether if the endpoint is opened.
     * For example, if the file is opened for [FileSink].
     */
    val isOpened: StateFlow<Boolean>

    /**
     * A info to verify supported formats.
     */
    val info: IEndpointInfo

    /**
     * Gets configuration information
     *
     * @param descriptor the media descriptor
     * @return the configuration information
     */
    fun getInfo(descriptor: MediaDescriptor) = getInfo(descriptor.type)

    /**
     * Gets configuration information
     *
     * @param type the media descriptor type
     * @return the configuration information
     */
    fun getInfo(type: MediaDescriptor.Type): IEndpointInfo

    interface IEndpointInfo {
        /**
         * A info to verify supported audio formats.
         */
        val audio: IAudioEndpointInfo

        interface IAudioEndpointInfo {
            /**
             * Supported audio encoders.
             */
            val supportedEncoders: List<String>

            /**
             * Supported audio sample rates.
             *
             * Returns null if not applicable (no limitation)
             */
            val supportedSampleRates: List<Int>?

            /**
             * Supported audio byte formats.
             *
             * Returns null if not applicable (no limitation)
             */
            val supportedByteFormats: List<Int>?
        }

        /**
         * A info to verify supported video formats.
         */
        val video: IVideoEndpointInfo

        interface IVideoEndpointInfo {
            /**
             * Supported video encoders.
             */
            val supportedEncoders: List<String>
        }
    }

    /**
     * Metrics of the endpoint.
     */
    val metrics: Any
}
