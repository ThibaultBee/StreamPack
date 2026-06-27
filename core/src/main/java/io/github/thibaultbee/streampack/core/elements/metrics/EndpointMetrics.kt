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
     *  The number of packets written.
     */
    val packetsWritten: Long

    /**
     * The number of packets dropped before writing (e.g. due to congestion or timeout).
     */
    val packetsWriteDropped: Long

    /**
     * The number of packets lost during the transmission.
     */
    val packetsWriteLost: Long

    /**
     * The number of bytes successfully written.
     */
    val bytesWritten: Long

    /**
     * The number of bytes dropped before writing (e.g. due to congestion or timeout).
     */
    val bytesWriteDropped: Long
    /**
     * Subtracts two [BasicEndpointMetrics]s.
     */
    operator fun minus(other: BasicEndpointMetrics): BasicEndpointMetrics {
        return object : BasicEndpointMetrics {
            override val uptime = this@BasicEndpointMetrics.uptime - other.uptime
            override val packetsWritten = this@BasicEndpointMetrics.packetsWritten - other.packetsWritten
            override val packetsWriteDropped = this@BasicEndpointMetrics.packetsWriteDropped - other.packetsWriteDropped
            override val packetsWriteLost = this@BasicEndpointMetrics.packetsWriteLost - other.packetsWriteLost
            override val bytesWritten = this@BasicEndpointMetrics.bytesWritten - other.bytesWritten
            override val bytesWriteDropped = this@BasicEndpointMetrics.bytesWriteDropped - other.bytesWriteDropped
        }
    }
}

/**
 * The total written bitrate in bits per second (bps).
 */
val BasicEndpointMetrics.writtenBitrateInBps: Long
    get() = uptime.inWholeMilliseconds.let { if (it == 0L) 0L else (bytesWritten * 8000) / it }

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