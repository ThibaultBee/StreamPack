package com.github.thibaultbee.srtstreamer.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.github.thibaultbee.srtstreamer.interfaces.EncoderGenerateExtraListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer


class AudioEncoder(logger: Logger) : Encoder(logger), EncoderGenerateExtraListener {

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
            return generateADTS(buffer, format)
        } else if (format.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_OPUS) {
            TODO("Not yet implemented")
        }
        return null
    }

    private fun samplingFrequencyIndex(samplingFrequency: Int): Int {
        return when (samplingFrequency) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            7350 -> 12
            else -> 15
        }
    }

    private fun channelConfiguration(channelCount: Int): Int {
        return when (channelCount) {
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 6
            8 -> 7
            else -> 0
        }
    }

    private fun generateADTS(buffer: ByteBuffer, format: MediaFormat): ByteBuffer {
        val protectionAbsent = 1
        val adts = ByteBuffer.allocateDirect(if (protectionAbsent == 1) 7 else 9) // No CRC protection
        adts.put(0xFF.toByte())
        adts.put((0xF0 or protectionAbsent).toByte()) // MPEG-4 - Layer 0 - protection absent

        var byte = (2 - 1) shl 6 // profile: audio_object_type - 1 (2-bit)
        val sampleRateIndex = samplingFrequencyIndex(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        byte = byte or (sampleRateIndex shl 2)
        val channelConfiguration = channelConfiguration(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        byte = byte or (channelConfiguration shr 2)
        adts.put(byte.toByte())

        byte = channelConfiguration shl 6
        val frameLength = buffer.limit() + if (protectionAbsent == 1) 7 else 9
        byte = byte or ((frameLength and 0x1800) shr 11)
        adts.put(byte.toByte())

        byte = (frameLength and 0x7F8) shr 3
        adts.put(byte.toByte())

        byte = (frameLength and 0x7) shl 5
        // Buffer fullness 0x7FF for variable
        byte = byte or 0x1F
        adts.put(byte.toByte())

        adts.put(0xFC.toByte())

        adts.rewind()
        return adts
    }
}