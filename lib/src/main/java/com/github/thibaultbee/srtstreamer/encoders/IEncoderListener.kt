package com.github.thibaultbee.srtstreamer.encoders

import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import java.nio.ByteBuffer

interface IEncoderListener {
    fun onInputFrame(buffer: ByteBuffer): Frame?
    fun onOutputFrame(frame: Frame): Error
}