package com.github.thibaultbee.streampack.encoders.format.avc

import com.github.thibaultbee.streampack.utils.BitBuffer
import com.github.thibaultbee.streampack.utils.putUE
import java.nio.ByteBuffer

class Sei(private val angle: Int) {
    companion object {
        const val H264_SEI_NAL = 6
        const val H264_SEI_TYPE_DISPLAY_ORIENTATION = 47
    }

    fun toByteBuffer(): ByteBuffer {
        val sei = BitBuffer.allocate(11 * Byte.SIZE_BITS.toLong())

        // Start code
        sei.put(1, 32)
        sei.put(false)
        sei.put(3, 2)
        sei.put(H264_SEI_NAL, 5)

        sei.put(H264_SEI_TYPE_DISPLAY_ORIENTATION, 8)
        sei.put(3, 8) // Payload size
        sei.put(false) // display_orientation_cancel_flag
        sei.put(false) // hor_flip
        sei.put(false) // ver_flip
        val anticlockwiseRotation = if (angle >= 0.0) {
            angle
        } else {
            angle + 360
        } * 65536 / 360
        sei.put(anticlockwiseRotation, 16)  // anticlockwise_rotation
        sei.putUE(1) // display_orientation_repetition_period
        sei.put(false)  // display_orientation_extension_flag

        sei.put(false)  // trailing
        sei.put(true)  // trailing

        return sei.toByteBuffer()
    }
}