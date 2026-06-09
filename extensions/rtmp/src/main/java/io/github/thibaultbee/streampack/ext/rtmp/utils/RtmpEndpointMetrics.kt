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
package io.github.thibaultbee.streampack.ext.rtmp.utils

import io.github.komedia.komuxer.rtmp.util.metrics.RtmpMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetrics
import kotlin.time.Duration

/**
 * Creates a [RtmpEndpointMetrics] from a [RtmpMetrics].
 */
fun RtmpEndpointMetrics(rawMetrics: RtmpMetrics): RtmpEndpointMetrics {
    return RtmpEndpointMetrics(
        uptime = rawMetrics.uptime,
        packetsSent = rawMetrics.messagesSent,
        packetsSendDropped = rawMetrics.messagesSendDropped,
        packetsSendLost = 0L,
        bytesSent = rawMetrics.totalBytesSent,
        bytesSendDropped = rawMetrics.payloadSendDroppedSize,
        rawMetrics = rawMetrics
    )
}

/**
 * Specific [EndpointMetrics] for RTMP protocol, based on [RtmpMetrics].
 */
data class RtmpEndpointMetrics(
    override val uptime: Duration,
    override val packetsSent: Long,
    override val packetsSendDropped: Long,
    override val packetsSendLost: Long,
    override val bytesSent: Long,
    override val bytesSendDropped: Long,
    override val rawMetrics: RtmpMetrics
) : EndpointMetrics<RtmpMetrics>
