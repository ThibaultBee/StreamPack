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
package io.github.thibaultbee.streampack.ext.srt.utils

import io.github.thibaultbee.srtdroid.core.models.Stats
import io.github.thibaultbee.srtdroid.ktx.CoroutineSrtSocket
import io.github.thibaultbee.streampack.core.elements.metrics.BasicEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetrics
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a [SrtEndpointMetrics] from a [SrtRawMetrics].
 */
fun SrtEndpointMetrics(rawMetrics: SrtRawMetrics): SrtEndpointMetrics {
    val stats = rawMetrics.bstats(clear = false)
    return SrtEndpointMetrics(
        uptime = stats.msTimeStamp.milliseconds,
        packetsSent = stats.pktSentTotal,
        packetsReceived = stats.pktRecvTotal,
        packetsSendLost = stats.pktSndLossTotal.toLong(),
        packetsReceiveLost = stats.pktRcvLossTotal,
        packetsRetransmitted = stats.pktRetransTotal,
        packetsSendACK = stats.pktSentACKTotal,
        packetsReceiveACK = stats.pktRecvACKTotal,
        packetsSendNAK = stats.pktSentNAKTotal,
        packetsReceiveNAK = stats.pktRecvNAKTotal,
        usSndDuration = stats.usSndDurationTotal,
        packetsSendDropped = stats.pktSndDropTotal.toLong(),
        packetsReceiveDropped = stats.pktRcvDropTotal,
        packetsReceiveUndecrypt = stats.pktRcvUndecryptTotal,
        bytesSent = stats.byteSentTotal,
        bytesReceived = stats.byteRecvTotal,
        bytesReceiveLost = stats.byteRcvLossTotal,
        bytesRetransmitted = stats.byteRetransTotal,
        bytesSendDropped = stats.byteSndDropTotal,
        bytesReceiveDropped = stats.byteRcvDropTotal,
        bytesReceiveUndecrypt = stats.byteRcvUndecryptTotal,
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
     * The total number of sent data packets, including retransmissions
     */
    override val packetsSent: Long
    /**
     * The total number of received packets
     */
    val packetsReceived: Long
    /**
     * The total number of lost packets (sender side)
     */
    override val packetsSendLost: Long
    /**
     * The total number of lost packets (receiver side)
     */
    val packetsReceiveLost: Int
    /**
     * The total number of retransmitted packets
     */
    val packetsRetransmitted: Int
    /**
     * The total number of sent ACK packets
     */
    val packetsSendACK: Int
    /**
     * The total number of received ACK packets
     */
    val packetsReceiveACK: Int
    /**
     * The total number of sent NAK packets
     */
    val packetsSendNAK: Int
    /**
     * The total number of received NAK packets
     */
    val packetsReceiveNAK: Int
    /**
     * The total time duration when UDT is sending data (idle time exclusive)
     */
    val usSndDuration: Long
    /**
     * The number of too-late-to-send dropped packets
     */
    override val packetsSendDropped: Long
    /**
     * The number of too-late-to play missing packets
     */
    val packetsReceiveDropped: Int
    /**
     * The number of undecrypted packets
     */
    val packetsReceiveUndecrypt: Int
    /**
     * The total number of sent data bytes, including retransmissions
     */
    override val bytesSent: Long
    /**
     * The total number of received bytes
     */
    val bytesReceived: Long
    /**
     * The total number of lost bytes
     */
    val bytesReceiveLost: Long
    /**
     * The total number of retransmitted bytes
     */
    val bytesRetransmitted: Long
    /**
     * The number of too-late-to-send dropped bytes
     */
    override val bytesSendDropped: Long
    /**
     * The number of too-late-to play missing bytes (estimate based on average packet size)
     */
    val bytesReceiveDropped: Long
    /**
     * The number of undecrypted bytes
     */
    val bytesReceiveUndecrypt: Long

    override operator fun minus(other: BasicEndpointMetrics): BasicEndpointMetrics {
        if (other !is SrtBasicEndpointMetrics) return super.minus(other)

        return object : SrtBasicEndpointMetrics {
            override val uptime = this@SrtBasicEndpointMetrics.uptime - other.uptime
            override val packetsSent = this@SrtBasicEndpointMetrics.packetsSent - other.packetsSent
            override val packetsReceived = this@SrtBasicEndpointMetrics.packetsReceived - other.packetsReceived
            override val packetsSendLost = this@SrtBasicEndpointMetrics.packetsSendLost - other.packetsSendLost
            override val packetsReceiveLost = this@SrtBasicEndpointMetrics.packetsReceiveLost - other.packetsReceiveLost
            override val packetsRetransmitted = this@SrtBasicEndpointMetrics.packetsRetransmitted - other.packetsRetransmitted
            override val packetsSendACK = this@SrtBasicEndpointMetrics.packetsSendACK - other.packetsSendACK
            override val packetsReceiveACK = this@SrtBasicEndpointMetrics.packetsReceiveACK - other.packetsReceiveACK
            override val packetsSendNAK = this@SrtBasicEndpointMetrics.packetsSendNAK - other.packetsSendNAK
            override val packetsReceiveNAK = this@SrtBasicEndpointMetrics.packetsReceiveNAK - other.packetsReceiveNAK
            override val usSndDuration = this@SrtBasicEndpointMetrics.usSndDuration - other.usSndDuration
            override val packetsSendDropped = this@SrtBasicEndpointMetrics.packetsSendDropped - other.packetsSendDropped
            override val packetsReceiveDropped = this@SrtBasicEndpointMetrics.packetsReceiveDropped - other.packetsReceiveDropped
            override val packetsReceiveUndecrypt = this@SrtBasicEndpointMetrics.packetsReceiveUndecrypt - other.packetsReceiveUndecrypt
            override val bytesSent = this@SrtBasicEndpointMetrics.bytesSent - other.bytesSent
            override val bytesReceived = this@SrtBasicEndpointMetrics.bytesReceived - other.bytesReceived
            override val bytesReceiveLost = this@SrtBasicEndpointMetrics.bytesReceiveLost - other.bytesReceiveLost
            override val bytesRetransmitted = this@SrtBasicEndpointMetrics.bytesRetransmitted - other.bytesRetransmitted
            override val bytesSendDropped = this@SrtBasicEndpointMetrics.bytesSendDropped - other.bytesSendDropped
            override val bytesReceiveDropped = this@SrtBasicEndpointMetrics.bytesReceiveDropped - other.bytesReceiveDropped
            override val bytesReceiveUndecrypt = this@SrtBasicEndpointMetrics.bytesReceiveUndecrypt - other.bytesReceiveUndecrypt
        }
    }
}



/**
 * Specific [EndpointMetrics] for SRT protocol, based on [SrtRawMetrics].
 */
data class SrtEndpointMetrics(
    override val uptime: Duration,
    override val packetsSent: Long,
    override val packetsReceived: Long,
    override val packetsSendLost: Long,
    override val packetsReceiveLost: Int,
    override val packetsRetransmitted: Int,
    override val packetsSendACK: Int,
    override val packetsReceiveACK: Int,
    override val packetsSendNAK: Int,
    override val packetsReceiveNAK: Int,
    override val usSndDuration: Long,
    override val packetsSendDropped: Long,
    override val packetsReceiveDropped: Int,
    override val packetsReceiveUndecrypt: Int,
    override val bytesSent: Long,
    override val bytesReceived: Long,
    override val bytesReceiveLost: Long,
    override val bytesRetransmitted: Long,
    override val bytesSendDropped: Long,
    override val bytesReceiveDropped: Long,
    override val bytesReceiveUndecrypt: Long,
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
        return socketProvider()?.bstats(clear)
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
        return socketProvider()?.bistats(clear, instantaneous)
    }
}

/**
 * Reports the current statistics.
 *
 * **See Also:** [srt_bstats](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_bstats)
 *
 * @param clear true if the statistics should be cleared after retrieval
 * @return the current [Stats] or [SrtStatsHelper.ZERO] if the socket is not connected
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