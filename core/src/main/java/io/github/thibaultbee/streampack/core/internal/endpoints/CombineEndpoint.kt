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
import io.github.thibaultbee.streampack.core.internal.utils.combineStates
import io.github.thibaultbee.streampack.core.internal.utils.extensions.union
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.StateFlow


/**
 * Combines multiple endpoints into one.
 *
 * @param endpoints Endpoints to combine
 */
fun CombineEndpoint(vararg endpoints: IEndpointInternal) = CombineEndpoint(endpoints.toList())

/**
 * Combines multiple endpoints into one.
 * This endpoint will write frames to all endpoints if they are opened.
 *
 * For example, you can combine a local endpoint and a remote endpoint to record and stream at the
 * same time.
 *
 * For specific behavior like reconnecting your remote endpoint, you can create a custom endpoint that
 * inherits from [CombineEndpoint] and override [open], [close], [startStream], [stopStream].
 */
open class CombineEndpoint(protected val endpointInternals: List<IEndpointInternal>) :
    IEndpointInternal {
    /**
     * Internal map of endpoint streamId to real streamIds
     */
    protected val endpointsToStreamIdsMap = mutableMapOf<Pair<IEndpoint, Int>, Int>()

    /**
     * List of [IEndpoint]s
     */
    val endpoints: List<IEndpoint>
        get() = endpointInternals

    /**
     * Whether at least one endpoint is open.
     * This is a combination of all endpoints' [IEndpoint.isOpen].
     *
     * To verify if a specific endpoint is open, use [IEndpoint.isOpen] of the endpoint.
     */
    override val isOpen: StateFlow<Boolean> =
        combineStates(*endpointInternals.map { it.isOpen }.toTypedArray()) { _ ->
            endpointInternals.any { it.isOpen.value }
        }

    /**
     * The union of all endpoints' [IEndpoint.IEndpointInfo].
     */
    override val info: IEndpoint.IEndpointInfo
        get() = endpointInternals.map { it.info }
            .reduce { acc, iEndpointInfo -> acc union iEndpointInfo }

    /**
     * The union of all endpoints' [IEndpoint.IEndpointInfo].
     */
    override fun getInfo(type: MediaDescriptor.Type): IEndpoint.IEndpointInfo {
        return endpointInternals.map { it.getInfo(type) }
            .reduce { acc, iEndpointInfo -> acc union iEndpointInfo }
    }

    /**
     * Throws [UnsupportedOperationException] because [CombineEndpoint] does not have metrics.
     *
     * Call [IEndpoint.metrics] on each endpoint to get their metrics.
     */
    override val metrics: Any
        get() = throw UnsupportedOperationException("CombineEndpoint does not have metrics.")

    private fun createNewStreamId(): Int {
        var i = 0
        while (endpointsToStreamIdsMap.keys.any { it.second == i }) {
            i++
        }
        return i
    }

    override fun addStream(streamConfig: Config): Int {
        val streamId = createNewStreamId()
        endpointInternals.forEach { endpoint ->
            endpointsToStreamIdsMap[Pair(endpoint, streamId)] = endpoint.addStream(streamConfig)
        }
        return streamId
    }

    override fun addStreams(streamConfigs: List<Config>): Map<Config, Int> {
        val streamIds = mutableMapOf<Config, Int>()
        streamConfigs.forEach { streamConfig ->
            val streamId = createNewStreamId()
            endpointInternals.forEach { endpoint ->
                endpointsToStreamIdsMap[Pair(endpoint, streamId)] = endpoint.addStream(streamConfig)
            }
            streamIds[streamConfig] = streamId
        }
        return streamIds
    }

    /**
     * Opens the endpoints.
     * For specific behavior like reconnecting your remote endpoint, you can override this method.
     *
     * If the [descriptor] is a [CombineDescriptor] and has the same number of descriptors as
     * endpoints, it opens each endpoint with the corresponding descriptor.
     *
     * If the [descriptor] is not a [CombineDescriptor] or has a different number of descriptors.
     * It throws an [IllegalArgumentException].
     *
     * @param descriptor Media descriptor
     */
    override suspend fun open(descriptor: MediaDescriptor) {
        if (descriptor !is CombineDescriptor) {
            throw IllegalArgumentException("CombineEndpoint only supports CombineDescriptor.")
        }
        require(descriptor.descriptors.size == endpointInternals.size) {
            "CombineDescriptor must have the same number of descriptors as endpoints."
        }
        endpointInternals.forEachIndexed { index, endpoint ->
            try {
                endpoint.open(descriptor.descriptors[index])
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to open endpoint $endpoint", t)
            }
        }
    }

    /**
     * Closes all endpoints
     */
    override suspend fun close() {
        endpointInternals.forEach { endpoint ->
            try {
                endpoint.close()
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to close endpoint $endpoint", t)
            }
        }
    }

    /**
     * Starts all endpoints
     */
    override suspend fun startStream() {
        endpointInternals.forEach { endpoint -> endpoint.startStream() }
    }

    /**
     * Stops all endpoints
     */
    override suspend fun stopStream() {
        endpointInternals.forEach { endpoint ->
            try {
                endpoint.stopStream()
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to stop endpoint $endpoint", t)
            }
        }
        endpointsToStreamIdsMap.clear()
    }

    /**
     * Writes frame to all opened endpoints.
     *
     * If all endpoints write fails, it throws the exception of the first endpoint that failed.
     */
    override suspend fun write(frame: Frame, streamPid: Int) {
        val currentBufferPos = frame.buffer.position()
        var numOfThrowable = 0
        var throwable: Throwable? = null

        endpointInternals.forEach { endpoint ->
            try {
                if (endpoint.isOpen.value) {
                    val endpointStreamId = endpointsToStreamIdsMap[Pair(endpoint, streamPid)]!!
                    endpoint.write(frame, endpointStreamId)

                    // Reset buffer position to write frame to next endpoint
                    frame.buffer.position(currentBufferPos)
                }
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to write frame to endpoint $endpoint", t)
                if (throwable == null) {
                    throwable = t
                }
                numOfThrowable++
            }
        }

        if (numOfThrowable == endpointInternals.size) {
            throw throwable!!
        }
    }

    companion object {
        private const val TAG = "CombineEndpoint"
    }

    /**
     * A [MediaDescriptor] that combines multiple [MediaDescriptor]s.
     */
    class CombineDescriptor(val descriptors: List<MediaDescriptor>) :
        MediaDescriptor(descriptors.first().type, emptyList()) {
        /**
         * The URI of the first descriptor.
         */
        override val uri = descriptors.first().uri
    }
}