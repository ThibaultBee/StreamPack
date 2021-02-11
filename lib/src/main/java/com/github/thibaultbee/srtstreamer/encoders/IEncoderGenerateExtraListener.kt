package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaFormat
import java.nio.ByteBuffer

interface IEncoderGenerateExtraListener {
    fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer?
}