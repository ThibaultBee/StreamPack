package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.data.VideoConfig
import com.github.thibaultbee.srtstreamer.listeners.OnErrorListener
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

class VideoMediaCodecEncoder(
    videoConfig: VideoConfig,
    encoderListener: IEncoderListener,
    override var onErrorListener: OnErrorListener?,
    logger: Logger
) :
    MediaCodecEncoder(encoderListener, videoConfig.startBitrate, logger) {
    override val mediaCodec: MediaCodec

    init {
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
        mediaCodec.setCallback(encoderCallback)
        mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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

    val intputSurface = mediaCodec.createInputSurface()
}