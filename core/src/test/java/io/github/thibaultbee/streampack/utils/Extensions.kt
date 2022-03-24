package io.github.thibaultbee.streampack.utils

import java.nio.ByteBuffer

/**
 * Returns ByteBuffer array even if [ByteBuffer.hasArray] returns false.
 *
 * @return [ByteArray] extracted from [ByteBuffer]
 */
fun ByteBuffer.extractArray(): ByteArray {
    return if (this.hasArray()) {
        if (isDirect) {
            this.array().sliceArray(
                IntRange(
                    4,
                    4 + limit() - 1
                ))
        } else {
            this.array()
        }
    } else {
        val byteArray = ByteArray(this.remaining())
        this.get(byteArray)
        byteArray
    }
}