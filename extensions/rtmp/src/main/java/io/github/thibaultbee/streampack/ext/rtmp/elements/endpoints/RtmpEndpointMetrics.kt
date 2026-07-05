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

import io.github.komedia.komuxer.rtmp.util.metrics.RtmpMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetrics
import kotlin.time.Duration

/**
 * Creates a [RtmpEndpointMetrics] from a [metricsProvider].
 */
fun RtmpEndpointMetrics(metricsProvider: () -> RtmpMetrics?): RtmpEndpointMetrics {
    return RtmpEndpointMetrics(RtmpRawMetrics(metricsProvider))
}

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
class RtmpRawMetrics internal constructor(private val metricsProvider: () -> RtmpMetrics?) {
    /**
     * Returns the [RtmpMetrics] if the client is available, otherwise null.
     */
    val rtmpMetricsOrNull: RtmpMetrics?
        get() = metricsProvider()
}

/**
 * Returns the [RtmpMetrics] if the client is available, otherwise [RtmpMetrics.ZERO].
 */
val RtmpRawMetrics.rtmpMetrics: RtmpMetrics
    get() = rtmpMetricsOrNull ?: RtmpMetrics.ZERO