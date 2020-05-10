package com.github.thibaultbee.srtstreamer.interfaces

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface EncoderGenerateExtraListener {
    fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer?
}