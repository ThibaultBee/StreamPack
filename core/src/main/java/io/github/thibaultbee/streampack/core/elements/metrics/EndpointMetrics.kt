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
package io.github.thibaultbee.streampack.core.elements.metrics

import kotlin.time.Duration

interface BasicEndpointMetrics {
    /**
     * The duration of the interval
     */
    val uptime: Duration

    /**
     *  The number of packets sent.
     */
    val packetsSent: Long

    /**
     * The number of packets dropped before sending (e.g. due to congestion or timeout).
     */
    val packetsSendDropped: Long

    /**
     * The number of packets lost during the transmission.
     */
    val packetsSendLost: Long

    /**
     * The number of bytes successfully sent.
     */
    val bytesSent: Long

    /**
     * The number of bytes dropped before sending (e.g. due to congestion or timeout).
     */
    val bytesSendDropped: Long
    /**
     * Subtracts two [BasicEndpointMetrics]s.
     */
    operator fun minus(other: BasicEndpointMetrics): BasicEndpointMetrics {
        return object : BasicEndpointMetrics {
            override val uptime = this@BasicEndpointMetrics.uptime - other.uptime
            override val packetsSent = this@BasicEndpointMetrics.packetsSent - other.packetsSent
            override val packetsSendDropped = this@BasicEndpointMetrics.packetsSendDropped - other.packetsSendDropped
            override val packetsSendLost = this@BasicEndpointMetrics.packetsSendLost - other.packetsSendLost
            override val bytesSent = this@BasicEndpointMetrics.bytesSent - other.bytesSent
            override val bytesSendDropped = this@BasicEndpointMetrics.bytesSendDropped - other.bytesSendDropped
        }
    }
}

/**
 * The total sent bitrate in bits per second (bps).
 */
val BasicEndpointMetrics.sentBitrateInBps: Long
    get() = uptime.inWholeMilliseconds.let { if (it == 0L) 0L else (bytesSent * 8000) / it }

/**
 * Endpoint metrics interface
 */
interface EndpointMetrics<out T : Any> : BasicEndpointMetrics {
    /**
     * The protocol-specific metrics wrapper.
     */
    val rawMetrics: T
}

/**
 * A specific [WithMetrics] for [EndpointMetrics].
 *
 * The members from [BasicEndpointMetrics] represent cumulative metrics.
 */
interface WithEndpointMetrics : WithMetrics<EndpointMetrics<*>>