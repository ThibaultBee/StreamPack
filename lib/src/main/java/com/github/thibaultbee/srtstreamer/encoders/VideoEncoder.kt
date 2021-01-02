package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Size
import android.view.Surface
import com.github.thibaultbee.srtstreamer.interfaces.EncoderGenerateExtraListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

class VideoEncoder(logger: Logger) : Encoder(logger), EncoderGenerateExtraListener {

    fun configure(
        mimeType: String,
        bitrate: Int,
        resolution: Size,
        fps: Int,
        rotation: Int = 90, // Portrait only
        profile: Int = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
        level: Int = MediaCodecInfo.CodecProfileLevel.AVCLevel6
    ): Error {
        val videoFormat = MediaFormat.createVideoFormat(
            mimeType,
            resolution.width,
            resolution.height
        )

        // Create codec
        val codec = createCodec(videoFormat)
        if (codec != null) {
            mediaCodec = codec
        } else {
            return Error.INVALID_PARAMETER
        }

        // Extended video format
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        videoFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1s between I frame
        /*  videoFormat.setInteger(MediaFormat.KEY_PROFILE, profile)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              videoFormat.setInteger(MediaFormat.KEY_LEVEL, level)
          }*/

        encoderGenerateExtraListener = this

        // Apply configuration
        mediaCodec!!.setCallback(encoderCallback)
        mediaCodec!!.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        return Error.SUCCESS
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer? {
        val sps = format.getByteBuffer("csd-0")
        val pps = format.getByteBuffer("csd-1")

        var byteBufferSize = sps?.limit() ?: 0
        byteBufferSize += pps?.limit() ?: 0

        val extra = ByteBuffer.allocateDirect(byteBufferSize)
        sps?.let { extra.put(it) }
        pps?.let { extra.put(it) }

        extra.rewind()
        return extra
    }

    fun getIntputSurface(): Surface? {
        return mediaCodec?.createInputSurface()
    }
}