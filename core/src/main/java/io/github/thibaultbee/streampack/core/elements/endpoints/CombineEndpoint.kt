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
package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.utils.combineStates
import io.github.thibaultbee.streampack.core.elements.utils.extensions.intersect
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.utils.MultiThrowable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Combines multiple endpoints into one.
 *
 * @param endpoints Endpoints to combine
 * @param coroutineDispatcher Coroutine dispatcher to use for frame writing
 */
fun CombineEndpoint(vararg endpoints: IEndpointInternal, coroutineDispatcher: CoroutineDispatcher) =
    CombineEndpoint(endpoints.toList(), coroutineDispatcher)

/**
 * Combines multiple endpoints into one.
 * This endpoint will write frames to all endpoints if they are opened.
 *
 * For example, you can combine a local endpoint and a remote endpoint to record and stream at the
 * same time.
 *
 * For specific behavior like reconnecting your remote endpoint, you can create a custom endpoint that
 * inherits from [CombineEndpoint] and override [open], [close], [startStream], [stopStream].
 *
 * @param endpointInternals List of endpoints to combine
 * @param coroutineDispatcher Coroutine dispatcher to use for frame writing
 */
open class CombineEndpoint(
    protected val endpointInternals: List<IEndpointInternal>,
    coroutineDispatcher: CoroutineDispatcher
) :
    IEndpointInternal {
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

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
     * This is a combination of all endpoints' [IEndpoint.isOpenFlow].
     *
     * To verify if a specific endpoint is open, use [IEndpoint.isOpenFlow] of the endpoint.
     */
    override val isOpenFlow: StateFlow<Boolean> =
        combineStates(*endpointInternals.map { it.isOpenFlow }.toTypedArray()) { _ ->
            endpointInternals.any { it.isOpenFlow.value }
        }

    /**
     * The union of all endpoints' [IEndpoint.IEndpointInfo].
     */
    override val info: IEndpoint.IEndpointInfo
        get() = endpointInternals.map { it.info }
            .reduce { acc, iEndpointInfo -> acc intersect iEndpointInfo }

    /**
     * The union of all endpoints' [IEndpoint.IEndpointInfo].
     */
    override fun getInfo(type: MediaDescriptor.Type): IEndpoint.IEndpointInfo {
        return endpointInternals.map { it.getInfo(type) }
            .reduce { acc, iEndpointInfo -> acc intersect iEndpointInfo }
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

    override fun addStream(streamConfig: CodecConfig): Int {
        val streamId = createNewStreamId()
        endpointInternals.forEach { endpoint ->
            endpointsToStreamIdsMap[Pair(endpoint, streamId)] = endpoint.addStream(streamConfig)
        }
        return streamId
    }

    override fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        val streamIds = mutableMapOf<CodecConfig, Int>()
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
    override suspend fun write(frame: Frame, streamPid: Int, onFrameProcessed: (() -> Unit)) {
        val throwables = mutableListOf<Throwable>()

        /**
         * Track the number of frames written and processed to call onFrameProcessed only once
         * when all endpoints have processed the frame.
         */
        val deferreds = mutableListOf<Deferred<*>>()

        endpointInternals.filter { it.isOpenFlow.value }.forEach { endpoint ->
            try {
                val deferred = CompletableDeferred<Unit>()
                val duplicatedFrame = frame.copy(rawBuffer = frame.rawBuffer.duplicate())
                val endpointStreamId = endpointsToStreamIdsMap[Pair(endpoint, streamPid)]!!

                deferreds += deferred
                endpoint.write(duplicatedFrame, endpointStreamId, { deferred.complete(Unit) })
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to get stream id for endpoint $endpoint", t)
                throwables += t
            }
        }

        coroutineScope.launch {
            deferreds.forEach { it.await() }
            onFrameProcessed()
        }

        if (throwables.isNotEmpty()) {
            throw MultiThrowable(throwables)
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

/**
 * A factory to build a [CombineEndpoint] from a varargs of [IEndpointInternal.Factory].
 */
fun CombineEndpointFactory(vararg endpointFactory: IEndpointInternal.Factory) =
    CombineEndpointFactory(endpointFactory.toList())

/**
 * A factory to build a [CombineEndpoint] from a list of [IEndpointInternal.Factory].
 */
class CombineEndpointFactory(private val endpointFactory: List<IEndpointInternal.Factory>) :
    IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal {
        return CombineEndpoint(
            endpointFactory.map { it.create(context, dispatcherProvider) },
            dispatcherProvider.default
        )
    }
}