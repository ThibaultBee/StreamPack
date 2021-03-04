package com.github.thibaultbee.streampack.encoders

import com.github.thibaultbee.streampack.data.Frame
import java.nio.ByteBuffer

interface IEncoderListener {
    /**
     * Calls when an encoder needs an input frame.
     * @param buffer ByteBuffer to fill. It comes from MediaCodec
     * @return frame with correct pts and buffer filled with an input buffer
     */
    fun onInputFrame(buffer: ByteBuffer): Frame

    /**
     * Calls when an encoder has generated an output frame.
     * @param frame Output frame with correct parameters and buffers
     */
    fun onOutputFrame(frame: Frame)
}