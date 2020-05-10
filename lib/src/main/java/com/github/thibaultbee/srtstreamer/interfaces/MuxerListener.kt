package com.github.thibaultbee.srtstreamer.interfaces

import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import java.nio.ByteBuffer

interface MuxerListener {
    fun onOutputFrame(buffer: ByteBuffer): Error
}