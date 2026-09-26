/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints

import io.github.komedia.komuxer.rtmp.client.RtmpClient
import io.github.komedia.komuxer.rtmp.messages.UserControl
import io.github.komedia.komuxer.rtmp.util.metrics.RtmpMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Creates a [RtmpEndpointMetrics] from a [RtmpMetrics].
 */
fun RtmpEndpointMetrics(rawMetrics: RtmpRawMetrics): RtmpEndpointMetrics {
    val metrics = rawMetrics.rtmpMetrics
    return RtmpEndpointMetrics(
        uptime = metrics.uptime,
        packetsWritten = metrics.messagesSent,
        packetsWriteDropped = metrics.messagesSendDropped,
        packetsWriteLost = 0L,
        bytesWritten = metrics.totalBytesSent,
        bytesWriteDropped = metrics.payloadSendDroppedSize,
        rawMetrics = rawMetrics
    )
}

/**
 * Specific [EndpointMetrics] for RTMP protocol, based on [RtmpMetrics].
 */
data class RtmpEndpointMetrics(
    override val uptime: Duration,
    override val packetsWritten: Long,
    override val packetsWriteDropped: Long,
    override val packetsWriteLost: Long,
    override val bytesWritten: Long,
    override val bytesWriteDropped: Long,
    override val rawMetrics: RtmpRawMetrics
) : EndpointMetrics<RtmpRawMetrics>


/**
 * Provides an access to internal RTMP metrics APIs.
 */
class RtmpRawMetrics internal constructor(
    private val clientProvider: () -> RtmpClient?,
    private val metricsProvider: () -> RtmpMetrics?
) {
    private val pingMutex = Mutex()

    /**
     * Returns the [RtmpMetrics] if the client is available, otherwise null.
     */
    val rtmpMetricsOrNull: RtmpMetrics?
        get() = metricsProvider() ?: clientProvider()?.metrics

    private suspend fun writePingInternal(): UserControl {
        val client = clientProvider() ?: throw IllegalStateException("RTMP client is not available")
        if (client.isClosed) {
            throw IllegalStateException("RTMP client is closed")
        }
        return client.writePing()
    }

    /**
     * Writes a ping request to the server and awaits the response.
     *
     * @return the ping response [UserControl]
     * @throws IllegalStateException if the RTMP client is not available or closed
     */
    suspend fun writePing(): UserControl = pingMutex.withLock {
        writePingInternal()
    }

    /**
     * Computes the round trip time (RTT) to the RTMP server using a ping request.
     *
     * If the server has not implemented the ping response, it will hang indefinitely.
     *
     * @return the measured [Duration], or null if the client is not available, closed, or an error occurred.
     */
    suspend fun rtt(): Duration = pingMutex.withLock {
        measureTime {
            writePingInternal()
        }
    }
}

/**
 * Returns the [RtmpMetrics] if the client is available, otherwise [RtmpMetrics.ZERO].
 */
val RtmpRawMetrics.rtmpMetrics: RtmpMetrics
    get() = rtmpMetricsOrNull ?: RtmpMetrics.ZERO