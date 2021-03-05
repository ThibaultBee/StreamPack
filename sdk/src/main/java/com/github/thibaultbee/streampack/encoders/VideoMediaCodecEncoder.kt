package com.github.thibaultbee.streampack.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.Logger
import java.nio.ByteBuffer
import java.security.InvalidParameterException

class VideoMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override var onErrorListener: OnErrorListener?,
    logger: Logger
) :
    MediaCodecEncoder(encoderListener, logger) {
    var intputSurface: Surface? = null

    fun set(videoConfig: VideoConfig) {
        val videoFormat = MediaFormat.createVideoFormat(
            videoConfig.mimeType,
            videoConfig.resolution.width,
            videoConfig.resolution.height
        )

        // Create codec
        val codec = createCodec(videoFormat)
        mediaCodec = codec

        // Extended video format
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoConfig.startBitrate)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoConfig.fps)
        videoFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1s between I frame
        /*  videoFormat.setInteger(MediaFormat.KEY_PROFILE, profile)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              videoFormat.setInteger(MediaFormat.KEY_LEVEL, level)
          }*/

        // Apply configuration
        mediaCodec?.let {
            it.setCallback(encoderCallback)
            it.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            intputSurface = it.createInputSurface()
        } ?: throw InvalidParameterException("Can't start video MediaCodec")
    }

    override fun close() {
        super.close()
        intputSurface?.release()
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer {
        val csd0 = format.getByteBuffer("csd-0")
        val csd1 = format.getByteBuffer("csd-1")

        var byteBufferSize = csd0?.limit() ?: 0
        byteBufferSize += csd1?.limit() ?: 0

        val extra = ByteBuffer.allocate(byteBufferSize)
        csd0?.let { extra.put(it) }
        csd1?.let { extra.put(it) }

        extra.rewind()
        return extra
    }
}