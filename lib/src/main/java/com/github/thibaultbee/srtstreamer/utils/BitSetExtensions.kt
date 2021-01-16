package com.github.thibaultbee.srtstreamer.utils

import java.nio.ByteBuffer
import java.util.*

fun BitSet.set(fromIndex: Int, toIndex: Int, value: Int) {
    val size = toIndex - fromIndex
    for (index in 0..size) {
        if (value and 1 shl index != 0) {
            this.set(fromIndex + index)
        }
    }
}

fun BitSet.set(fromIndex: Int, toIndex: Int, value: Short) {
    val size = toIndex - fromIndex
    for (index in 0..size) {
        if (value.toInt() and 1 shl index != 0) {
            this.set(fromIndex + index)
        }
    }
}

fun BitSet.set(fromIndex: Int, toIndex: Int, value: Byte) {
    val size = toIndex - fromIndex
    for (index in 0..size) {
        if (value.toInt() and 1 shl index != 0) {
            this.set(fromIndex + index)
        }
    }
}

fun BitSet.toByteBuffer(): ByteBuffer {
    return ByteBuffer.wrap(this.toByteArray()) // TODO: avoid buffer copy. We need to use a custom BitSet like class
}