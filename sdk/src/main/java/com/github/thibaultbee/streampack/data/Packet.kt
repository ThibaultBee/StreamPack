package com.github.thibaultbee.streampack.data

import java.nio.ByteBuffer

data class Packet(
    var buffer: ByteBuffer,
    var isFirstPacketFrame: Boolean,
    var isLastPacketFrame: Boolean,
    var ts: Long, // in µs
)