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
package io.github.thibaultbee.streampack.ext.srt.elements.endpoints

import io.github.thibaultbee.srtdroid.core.models.Stats
import io.github.thibaultbee.srtdroid.ktx.CoroutineSrtSocket
import io.github.thibaultbee.streampack.core.elements.metrics.BasicEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetrics
import io.github.thibaultbee.streampack.ext.srt.utils.SrtStatsHelper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a [SrtEndpointMetrics] from a [SrtRawMetrics].
 */
fun SrtEndpointMetrics(rawMetrics: SrtRawMetrics): SrtEndpointMetrics {
    val stats = rawMetrics.bstats(clear = false)
    return SrtEndpointMetrics(
        uptime = stats.msTimeStamp.milliseconds,
        packetsWritten = stats.pktSentTotal,
        packetsRead = stats.pktRecvTotal,
        packetsWriteLost = stats.pktSndLossTotal.toLong(),
        packetsReadLost = stats.pktRcvLossTotal,
        packetsRetransmitted = stats.pktRetransTotal,
        packetsWriteACK = stats.pktSentACKTotal,
        packetsReadACK = stats.pktRecvACKTotal,
        packetsWriteNAK = stats.pktSentNAKTotal,
        packetsReadNAK = stats.pktRecvNAKTotal,
        usWriteDuration = stats.usSndDurationTotal,
        packetsWriteDropped = stats.pktSndDropTotal.toLong(),
        packetsReadDropped = stats.pktRcvDropTotal,
        packetsReadUndecrypt = stats.pktRcvUndecryptTotal,
        bytesWritten = stats.byteSentTotal,
        bytesRead = stats.byteRecvTotal,
        bytesReadLost = stats.byteRcvLossTotal,
        bytesRetransmitted = stats.byteRetransTotal,
        bytesWriteDropped = stats.byteSndDropTotal,
        bytesReadDropped = stats.byteRcvDropTotal,
        bytesReadUndecrypt = stats.byteRcvUndecryptTotal,
        rawMetrics = rawMetrics
    )
}

/**
 * Basic metrics for SRT protocol, without the raw metrics helper.
 */
interface SrtBasicEndpointMetrics : BasicEndpointMetrics {
    /**
     * The time since the entity is started
     */
    override val uptime: Duration
    /**
     * The total number of written data packets, including retransmissions
     */
    override val packetsWritten: Long
    /**
     * The total number of read packets
     */
    val packetsRead: Long
    /**
     * The total number of lost packets (writer side)
     */
    override val packetsWriteLost: Long
    /**
     * The total number of lost packets (reader side)
     */
    val packetsReadLost: Int
    /**
     * The total number of retransmitted packets
     */
    val packetsRetransmitted: Int
    /**
     * The total number of written ACK packets
     */
    val packetsWriteACK: Int
    /**
     * The total number of read ACK packets
     */
    val packetsReadACK: Int
    /**
     * The total number of written NAK packets
     */
    val packetsWriteNAK: Int
    /**
     * The total number of read NAK packets
     */
    val packetsReadNAK: Int
    /**
     * The total time duration when UDT is writing data (idle time exclusive)
     */
    val usWriteDuration: Long
    /**
     * The number of too-late-to-write dropped packets
     */
    override val packetsWriteDropped: Long
    /**
     * The number of too-late-to-play missing packets
     */
    val packetsReadDropped: Int
    /**
     * The number of undecrypted packets
     */
    val packetsReadUndecrypt: Int
    /**
     * The total number of written data bytes, including retransmissions
     */
    override val bytesWritten: Long
    /**
     * The total number of read bytes
     */
    val bytesRead: Long
    /**
     * The total number of lost bytes
     */
    val bytesReadLost: Long
    /**
     * The total number of retransmitted bytes
     */
    val bytesRetransmitted: Long
    /**
     * The number of too-late-to-write dropped bytes
     */
    override val bytesWriteDropped: Long
    /**
     * The number of too-late-to-play missing bytes (estimate based on average packet size)
     */
    val bytesReadDropped: Long
    /**
     * The number of undecrypted bytes
     */
    val bytesReadUndecrypt: Long

    override operator fun minus(other: BasicEndpointMetrics): BasicEndpointMetrics {
        if (other !is SrtBasicEndpointMetrics) return super.minus(other)

        return object : SrtBasicEndpointMetrics {
            override val uptime = this@SrtBasicEndpointMetrics.uptime - other.uptime
            override val packetsWritten = this@SrtBasicEndpointMetrics.packetsWritten - other.packetsWritten
            override val packetsRead = this@SrtBasicEndpointMetrics.packetsRead - other.packetsRead
            override val packetsWriteLost = this@SrtBasicEndpointMetrics.packetsWriteLost - other.packetsWriteLost
            override val packetsReadLost = this@SrtBasicEndpointMetrics.packetsReadLost - other.packetsReadLost
            override val packetsRetransmitted = this@SrtBasicEndpointMetrics.packetsRetransmitted - other.packetsRetransmitted
            override val packetsWriteACK = this@SrtBasicEndpointMetrics.packetsWriteACK - other.packetsWriteACK
            override val packetsReadACK = this@SrtBasicEndpointMetrics.packetsReadACK - other.packetsReadACK
            override val packetsWriteNAK = this@SrtBasicEndpointMetrics.packetsWriteNAK - other.packetsWriteNAK
            override val packetsReadNAK = this@SrtBasicEndpointMetrics.packetsReadNAK - other.packetsReadNAK
            override val usWriteDuration = this@SrtBasicEndpointMetrics.usWriteDuration - other.usWriteDuration
            override val packetsWriteDropped = this@SrtBasicEndpointMetrics.packetsWriteDropped - other.packetsWriteDropped
            override val packetsReadDropped = this@SrtBasicEndpointMetrics.packetsReadDropped - other.packetsReadDropped
            override val packetsReadUndecrypt = this@SrtBasicEndpointMetrics.packetsReadUndecrypt - other.packetsReadUndecrypt
            override val bytesWritten = this@SrtBasicEndpointMetrics.bytesWritten - other.bytesWritten
            override val bytesRead = this@SrtBasicEndpointMetrics.bytesRead - other.bytesRead
            override val bytesReadLost = this@SrtBasicEndpointMetrics.bytesReadLost - other.bytesReadLost
            override val bytesRetransmitted = this@SrtBasicEndpointMetrics.bytesRetransmitted - other.bytesRetransmitted
            override val bytesWriteDropped = this@SrtBasicEndpointMetrics.bytesWriteDropped - other.bytesWriteDropped
            override val bytesReadDropped = this@SrtBasicEndpointMetrics.bytesReadDropped - other.bytesReadDropped
            override val bytesReadUndecrypt = this@SrtBasicEndpointMetrics.bytesReadUndecrypt - other.bytesReadUndecrypt
        }
    }
}

/**
 * Specific [EndpointMetrics] for SRT protocol, based on [SrtRawMetrics].
 */
data class SrtEndpointMetrics(
    override val uptime: Duration,
    override val packetsWritten: Long,
    override val packetsRead: Long,
    override val packetsWriteLost: Long,
    override val packetsReadLost: Int,
    override val packetsRetransmitted: Int,
    override val packetsWriteACK: Int,
    override val packetsReadACK: Int,
    override val packetsWriteNAK: Int,
    override val packetsReadNAK: Int,
    override val usWriteDuration: Long,
    override val packetsWriteDropped: Long,
    override val packetsReadDropped: Int,
    override val packetsReadUndecrypt: Int,
    override val bytesWritten: Long,
    override val bytesRead: Long,
    override val bytesReadLost: Long,
    override val bytesRetransmitted: Long,
    override val bytesWriteDropped: Long,
    override val bytesReadDropped: Long,
    override val bytesReadUndecrypt: Long,
    /**
     * Raw SRT socket helper
     */
    override val rawMetrics: SrtRawMetrics,
) : SrtBasicEndpointMetrics, EndpointMetrics<SrtRawMetrics>

/**
 * Provides an access to internal SRT APIs.
 */
class SrtRawMetrics internal constructor(private val socketProvider: () -> CoroutineSrtSocket?) {
    /**
     * Reports the current statistics.
     *
     * **See Also:** [srt_bstats](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_bstats)
     *
     * @param clear true if the statistics should be cleared after retrieval
     * @return the current [Stats] or null if the socket is not connected
     */
    fun bstatsOrNull(clear: Boolean): Stats? {
        val socket = socketProvider() ?: return null
        if (!socket.isConnected) return null
        return socket.bstats(clear)
    }

    /**
     * Reports the current statistics.
     *
     * **See Also:** [srt_bistats](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_bistats)
     *
     * @param clear true if the statistics should be cleared after retrieval
     * @param instantaneous true if the statistics should use instant data, not moving averages
     * @return the current [Stats] if the socket is not connected
     */
    fun bistatsOrNull(clear: Boolean, instantaneous: Boolean): Stats? {
        val socket = socketProvider() ?: return null
        if (!socket.isConnected) return null
        return socket.bistats(clear, instantaneous)
    }
}

/**
 * Reports the current statistics.
 *
 * **See Also:** [srt_bstats](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_bstats)
 *
 * @param clear true if the statistics should be cleared after retrieval
 * @return the current [Stats] or [io.github.thibaultbee.streampack.ext.srt.utils.SrtStatsHelper.ZERO] if the socket is not connected
 */
fun SrtRawMetrics.bstats(clear: Boolean): Stats {
    return bstatsOrNull(clear) ?: SrtStatsHelper.ZERO
}

/**
 * Reports the current statistics.
 *
 * **See Also:** [srt_bistats](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_bistats)
 *
 * @param clear true if the statistics should be cleared after retrieval
 * @param instantaneous true if the statistics should use instant data, not moving averages
 * @return the current [Stats] or [SrtStatsHelper.ZERO] if the socket is not connected
 */
fun SrtRawMetrics.bistats(clear: Boolean, instantaneous: Boolean): Stats {
    return bistatsOrNull(clear, instantaneous) ?: SrtStatsHelper.ZERO
}