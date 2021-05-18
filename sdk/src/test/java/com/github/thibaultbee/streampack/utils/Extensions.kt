package com.github.thibaultbee.streampack.utils

import java.nio.ByteBuffer

/**
 * Returns ByteBuffer array even if [ByteBuffer.hasArray] returns false.
 *
 * @return [ByteArray] extracted from [ByteBuffer]
 */
fun ByteBuffer.extractArray(): ByteArray {
    return if (this.hasArray()) {
        this.array()
    } else {
        val byteArray = ByteArray(this.remaining())
        this.get(byteArray)
        byteArray
    }
}