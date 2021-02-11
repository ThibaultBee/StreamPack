package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.github.thibaultbee.srtstreamer.encoders.format.Adts
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer


class AudioEncoder(logger: Logger) : Encoder(logger), IEncoderGenerateExtraListener {

    fun configure(
        mimeType: String,
        bitrate: Int,
        sampleRate: Int,
        nChannel: Int,
        audioByteFormat: Int,
        aacCodecProfileLevel: Int = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    ): Error {
        val audioFormat = MediaFormat.createAudioFormat(
            mimeType,
            sampleRate,
            nChannel
        )

        // Create codec
        val codec = createCodec(audioFormat)
        if (codec != null) {
            mediaCodec = codec
        } else {
            return Error.INVALID_PARAMETER
        }

        // Extended audio format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioFormat.setInteger(
                MediaFormat.KEY_PCM_ENCODING,
                audioByteFormat
            )
        }
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        if (mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, aacCodecProfileLevel)
        }
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)

        encoderGenerateExtraListener = this

        // Apply configuration
        mediaCodec!!.setCallback(encoderCallback)
        mediaCodec!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        return Error.SUCCESS
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer? {
        if (format.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC) {
            return Adts(format, buffer.limit()).asByteBuffer()
        } else if (format.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_OPUS) {
            TODO("Not yet implemented")
        }
        return null
    }

}