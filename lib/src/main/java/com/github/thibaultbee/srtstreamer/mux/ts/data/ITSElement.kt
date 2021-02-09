package com.github.thibaultbee.srtstreamer.mux.ts.data

import java.nio.ByteBuffer

interface ITSElement {
    val size: Int
    val bitSize: Int

    fun asByteBuffer(): ByteBuffer
}