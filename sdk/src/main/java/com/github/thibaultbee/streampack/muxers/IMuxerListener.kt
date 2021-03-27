package com.github.thibaultbee.streampack.muxers

import com.github.thibaultbee.streampack.data.Packet

interface IMuxerListener {
    fun onOutputFrame(packet: Packet)
}