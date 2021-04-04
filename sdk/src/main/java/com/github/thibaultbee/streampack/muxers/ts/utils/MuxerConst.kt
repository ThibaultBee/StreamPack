package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.muxers.IMuxerListener

object MuxerConst {
    const val PAT_PACKET_PERIOD = 40
    const val SDT_PACKET_PERIOD = 200

    /**
     * Number of MPEG-TS packet stream in output [Packet] returns by [IMuxerListener.onOutputFrame]
     */
    const val MAX_OUTPUT_PACKET_NUMBER = 7
}