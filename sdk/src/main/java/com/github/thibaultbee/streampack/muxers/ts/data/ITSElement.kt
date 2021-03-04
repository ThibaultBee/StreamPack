package com.github.thibaultbee.streampack.muxers.ts.data

import java.nio.ByteBuffer

interface ITSElement {
    val size: Int
    val bitSize: Int

    fun asByteBuffer(): ByteBuffer
}