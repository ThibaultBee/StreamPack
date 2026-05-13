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

object SrtStatsHelper {
    val ZERO = Stats(
        msTimeStamp = 0L,
        pktSentTotal = 0L,
        pktRecvTotal = 0L,
        pktSndLossTotal = 0,
        pktRcvLossTotal = 0,
        pktRetransTotal = 0,
        pktSentACKTotal = 0,
        pktRecvACKTotal = 0,
        pktSentNAKTotal = 0,
        pktRecvNAKTotal = 0,
        usSndDurationTotal = 0,
        pktSndDropTotal = 0,
        pktRcvDropTotal = 0,
        pktRcvUndecryptTotal = 0,
        byteSentTotal = 0,
        byteRecvTotal = 0,
        byteRcvLossTotal = 0,
        byteRetransTotal = 0,
        byteSndDropTotal = 0,
        byteRcvDropTotal = 0,
        byteRcvUndecryptTotal = 0,
        pktSent = 0,
        pktRecv = 0,
        pktSndLoss = 0,
        pktRcvLoss = 0,
        pktRetrans = 0,
        pktRcvRetrans = 0,
        pktSentACK = 0,
        pktRecvACK = 0,
        pktSentNAK = 0,
        pktRecvNAK = 0,
        mbpsSendRate = 0.0,
        mbpsRecvRate = 0.0,
        usSndDuration = 0,
        pktReorderDistance = 0,
        pktRcvAvgBelatedTime = 0.0,
        pktRcvBelated = 0,
        pktSndDrop = 0,
        pktRcvDrop = 0,
        pktRcvUndecrypt = 0,
        byteSent = 0,
        byteRecv = 0,
        byteRcvLoss = 0,
        byteRetrans = 0,
        byteSndDrop = 0,
        byteRcvDrop = 0,
        byteRcvUndecrypt = 0,
        usPktSndPeriod = 0.0,
        pktFlowWindow = 0,
        pktCongestionWindow = 0,
        pktFlightSize = 0,
        msRTT = 0.0,
        mbpsBandwidth = 0.0,
        byteAvailSndBuf = 0,
        byteAvailRcvBuf = 0,
        mbpsMaxBW = 0.0,
        byteMSS = 0,
        pktSndBuf = 0,
        byteSndBuf = 0,
        msSndBuf = 0,
        msSndTsbPdDelay = 0,
        pktRcvBuf = 0,
        byteRcvBuf = 0,
        msRcvBuf = 0,
        msRcvTsbPdDelay = 0,
        pktSndFilterExtraTotal = 0,
        pktRcvFilterExtraTotal = 0,
        pktRcvFilterSupplyTotal = 0,
        pktRcvFilterLossTotal = 0,
        pktSndFilterExtra = 0,
        pktRcvFilterExtra = 0,
        pktRcvFilterSupply = 0,
        pktRcvFilterLoss = 0,
        pktReorderTolerance = 0,
        pktSentUniqueTotal = 0,
        pktRecvUniqueTotal = 0,
        byteSentUniqueTotal = 0,
        byteRecvUniqueTotal = 0,
        pktSentUnique = 0,
        pktRecvUnique = 0,
        byteSentUnique = 0,
        byteRecvUnique = 0
    )
}


