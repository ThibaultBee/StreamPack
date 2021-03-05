package com.github.thibaultbee.streampack.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import com.github.thibaultbee.streampack.data.Frame
import com.github.thibaultbee.streampack.utils.Error
import com.github.thibaultbee.streampack.utils.EventHandlerManager
import com.github.thibaultbee.streampack.utils.Logger
import java.nio.ByteBuffer
import java.security.InvalidParameterException

abstract class MediaCodecEncoder(
    override val encoderListener: IEncoderListener,
    val logger: Logger
) :
    EventHandlerManager(), IEncoder {
    protected var mediaCodec: MediaCodec? = null
    var bitrate = 0
        set(value) {
            val bundle = Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, value)
            mediaCodec?.setParameters(bundle)
            field = value
        }

    protected val encoderCallback = object : MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            /**
             * An IllegalStateException happens when MediaCodec is stopped. Dirty fix: catch it...
             */
            try {
                mediaCodec?.let { mediaCodec ->
                    mediaCodec.getOutputBuffer(index)?.let { buffer ->
                        val extra = onGenerateExtra(buffer, codec.outputFormat)
                        Frame(
                            buffer,
                            codec.outputFormat.getString(MediaFormat.KEY_MIME)!!,
                            info.presentationTimeUs, // pts
                            null, // dts
                            info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME,
                            info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG,
                            extra
                            /**
                             * Drop codec data. They are already pass in the extra buffer.
                             */
                        ).takeUnless { it.isCodecData }?.let {
                            encoderListener.onOutputFrame(
                                it
                            )
                        }

                        mediaCodec.releaseOutputBuffer(index, false)
                    } ?: reportError(Error.INVALID_BUFFER)
                }
            } catch (e: IllegalStateException) {
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            /**
             * An IllegalStateException happens when MediaCodec is stopped. Dirty fix: catch it...
             */
            try {
                mediaCodec?.let { mediaCodec ->
                    mediaCodec.getInputBuffer(index)?.let { buffer ->
                        val frame = encoderListener.onInputFrame(buffer)
                        frame.let {
                            mediaCodec.queueInputBuffer(
                                index,
                                0,
                                it.buffer.remaining(),
                                it.pts /* in us */,
                                0
                            )
                        }
                    } ?: reportError(Error.INVALID_BUFFER)
                }
            } catch (e: IllegalStateException) {
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            logger.i(this, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            reportError(e)
        }
    }

    fun createCodec(format: MediaFormat): MediaCodec {
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val encoderName = mediaCodecList.findEncoderForFormat(format)
        encoderName?.let { return MediaCodec.createByCodecName(encoderName) }
            ?: throw InvalidParameterException("Failed to create codec for $mediaCodec")
    }

    override val mimeType: String?
        get() = mediaCodec?.outputFormat?.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalStateException("Can't get MimeType without configuration")

    override fun startStream() {
        mediaCodec?.start() ?: throw IllegalStateException("Can't start without configuration")
    }

    override fun stopStream() {
        try {
            mediaCodec?.setCallback(null)
            mediaCodec?.signalEndOfInputStream()
            mediaCodec?.flush()
            mediaCodec?.stop()
        } catch (e: IllegalStateException) {
            logger.d(this, "Not running")
        }
    }

    override fun release() {
        mediaCodec?.release()
        mediaCodec = null
    }

    abstract fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer

    companion object {
        fun getEncoders(): List<MediaCodecInfo> {
            val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            return mediaCodecList.codecInfos
                .filter { codec -> codec.isEncoder }
                .toList()
        }
    }
}