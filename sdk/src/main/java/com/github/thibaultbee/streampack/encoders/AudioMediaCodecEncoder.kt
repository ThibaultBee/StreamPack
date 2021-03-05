package com.github.thibaultbee.streampack.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.encoders.format.aac.Adts
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.Logger
import java.nio.ByteBuffer
import java.security.InvalidParameterException


class AudioMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override var onErrorListener: OnErrorListener?,
    logger: Logger
) :
    MediaCodecEncoder(encoderListener, logger) {

    fun set(audioConfig: AudioConfig) {
        val audioFormat = MediaFormat.createAudioFormat(
            audioConfig.mimeType,
            audioConfig.sampleRate,
            AudioConfig.getChannelNumber(audioConfig.channelConfig)
        )

        // Create codec
        val codec = createCodec(audioFormat)
        mediaCodec = codec

        // Extended audio format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioFormat.setInteger(
                MediaFormat.KEY_PCM_ENCODING,
                audioConfig.audioByteFormat
            )
        }
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioConfig.startBitrate)
        if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            audioFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
        }
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)

        // Apply configuration
        mediaCodec?.let {
            it.setCallback(encoderCallback)
            it.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } ?: throw InvalidParameterException("Can't start audio MediaCodec")
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer {
        when (val mimeType = format.getString(MediaFormat.KEY_MIME)) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                return Adts(format, buffer.limit()).asByteBuffer()
            }
            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                TODO("Not yet implemented")
            }
            else -> {
                throw InvalidParameterException("Format is not supported: $mimeType")
            }
        }
    }

}