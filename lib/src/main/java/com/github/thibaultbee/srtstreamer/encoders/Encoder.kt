package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import com.github.thibaultbee.srtstreamer.interfaces.EncoderGenerateExtraListener
import com.github.thibaultbee.srtstreamer.interfaces.EncoderListener
import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger

open class Encoder(val logger: Logger): EventHandlerManager() {
    protected var mediaCodec: MediaCodec? = null
    var encoderListener: EncoderListener? = null
    lateinit var encoderGenerateExtraListener: EncoderGenerateExtraListener

    var bitrate = 0
        set(value) {
            val bundle = Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, value)
            mediaCodec?.setParameters(bundle)
            field = value
        }

    protected val encoderCallback = object: MediaCodec.Callback() {
        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            val buffer = mediaCodec!!.getOutputBuffer(index)
            if (buffer != null) {
                val extra = encoderGenerateExtraListener.onGenerateExtra(buffer, codec.outputFormat)
                encoderListener?.onOutputFrame(Frame(buffer,
                    codec.outputFormat.getString(MediaFormat.KEY_MIME)!!,
                    info.presentationTimeUs, // pts
                    info.presentationTimeUs, // dts
                    info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME,
                    info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG,
                    extra))
            } else {
                reportError(Error.INVALID_BUFFER)
            }
            try {
                mediaCodec!!.releaseOutputBuffer(index, false)
            } catch (e: IllegalStateException) {}
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            val buffer = mediaCodec!!.getInputBuffer(index)
            if (buffer != null) {
                val frame = encoderListener?.onInputFrame(buffer)
                if (frame != null) {
                    mediaCodec!!.queueInputBuffer(
                        index,
                        0,
                        frame.buffer.remaining(),
                        frame.pts /* in us */,
                        0
                    )
                }
            } else {
                reportError(Error.INVALID_BUFFER)
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            logger.i(this, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            reportError(Error.UNKNOWN)
            logger.e(this, e.diagnosticInfo)
        }
    }

    fun createCodec(format: MediaFormat): MediaCodec? {
        // Create codec
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val encoderName = mediaCodecList.findEncoderForFormat(format)
        if (encoderName == null) {
            logger.e(this, "Failed to get encoder for ${format.getString(MediaFormat.KEY_MIME)}")
        } else {
            return MediaCodec.createByCodecName(encoderName)
        }
        return null
    }

    fun getMimeType(): String? {
        return mediaCodec?.outputFormat?.getString(MediaFormat.KEY_MIME)
    }

    fun getIntputSurface(): Surface? {
        return mediaCodec?.createInputSurface()
    }

    fun start(): Error {
        return if (mediaCodec != null) {
            mediaCodec!!.start()
            Error.SUCCESS
        } else {
            Error.BAD_STATE
        }
    }

    fun stop(): Error {
         try {
            mediaCodec?.flush()
            mediaCodec?.stop()
        } catch (e: IllegalStateException) {
            logger.d(this, "Not running")
        }

         return Error.SUCCESS
    }

    fun release(): Error {
        mediaCodec?.release()
        mediaCodec = null

        return Error.SUCCESS
    }

    companion object {
        fun getEncoders(): List<MediaCodecInfo> {
            val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            return mediaCodecList.codecInfos
                .filter { codec -> codec.isEncoder }
                .toList()
        }
    }
}