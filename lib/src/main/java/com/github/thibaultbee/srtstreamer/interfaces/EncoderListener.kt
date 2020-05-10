package com.github.thibaultbee.srtstreamer.interfaces

import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import java.nio.ByteBuffer

interface EncoderListener {
    fun onInputFrame(buffer: ByteBuffer): Frame?
    fun onOutputFrame(frame: Frame): Error
}