package com.github.thibaultbee.srtstreamer.sources

import com.github.thibaultbee.srtstreamer.data.Frame
import com.github.thibaultbee.srtstreamer.interfaces.Controllable
import java.nio.ByteBuffer

interface ICapture : Controllable {

    /**
     * Generate a frame from capture device
     * @param buffer buffer where to write data. Must be set as buffer of returned Frame
     * @return frame with correct infos (at least buffer, mime type and pts)
     */
    fun getFrame(buffer: ByteBuffer): Frame
}