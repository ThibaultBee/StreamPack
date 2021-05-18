/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.internal.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.internal.encoders.format.aac.Adts
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.ILogger
import java.nio.ByteBuffer
import java.security.InvalidParameterException


class AudioMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override val onInternalErrorListener: OnErrorListener,
    logger: ILogger
) :
    MediaCodecEncoder(encoderListener, logger) {

    fun configure(audioConfig: AudioConfig) {
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
                audioConfig.byteFormat
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                createHandler("AMediaCodecThread")
                it.setCallback(encoderCallback, handler)
            } else {
                it.setCallback(encoderCallback)
            }
            it.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } ?: throw InvalidParameterException("Can't start audio MediaCodec")
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer {
        when (val mimeType = format.getString(MediaFormat.KEY_MIME)) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                return Adts(format, buffer.limit()).toByteBuffer()
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