/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.tags

import android.media.AudioFormat
import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.internal.utils.av.audio.AudioSpecificConfig
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import java.io.IOException
import java.nio.ByteBuffer

class AudioTag(
    pts: Long,
    private val frameBuffer: ByteBuffer,
    private val aacPacketType: AACPacketType?,
    private val audioConfig: AudioConfig
) :
    FlvTag(pts, TagType.AUDIO) {
    companion object {
        private const val AUDIO_TAG_HEADER_SIZE = 1
    }

    override fun writeTagHeader(output: ByteBuffer) {
        output.put(
            (SoundFormat.fromMimeType(audioConfig.mimeType).value shl 4) or
                    (SoundRate.fromSampleRate(audioConfig.sampleRate).value shl 2) or
                    (SoundSize.fromByteFormat(audioConfig.byteFormat).value shl 1) or
                    (SoundType.fromChannelConfig(audioConfig.channelConfig).value)
        )
        if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            output.put(aacPacketType!!.value)
        }
    }

    override val tagHeaderSize = computeHeaderSize()

    private fun computeHeaderSize(): Int {
        var size = AUDIO_TAG_HEADER_SIZE
        if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            size += 1 // AACPacketType
        }
        return size
    }

    override fun writeBody(output: ByteBuffer) {
        if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            if (aacPacketType == AACPacketType.SEQUENCE_HEADER) {
                AudioSpecificConfig.writeFromByteBuffer(output, frameBuffer, audioConfig)
            } else {
                output.put(frameBuffer)
            }
        } else {
            output.put(frameBuffer)
        }
    }

    override val bodySize = frameBuffer.remaining()
}

enum class SoundFormat(val value: Int) {
    PCM(0),
    ADPCM(1),
    MP3(2),
    PCM_LE(3),
    NELLYMOSER_16KHZ(4),
    NELLYMOSER_8KHZ(5),
    NELLYMOSER(6),
    G711_ALAW(7),
    G711_MLAW(8),
    AAC(10),
    SPEEX(11),
    MP3_8K(14),
    DEVICE_SPECIFIC(15);

    fun toMimeType() = when (this) {
        PCM -> MediaFormat.MIMETYPE_AUDIO_RAW
        G711_ALAW -> MediaFormat.MIMETYPE_AUDIO_G711_ALAW
        G711_MLAW -> MediaFormat.MIMETYPE_AUDIO_G711_MLAW
        AAC -> MediaFormat.MIMETYPE_AUDIO_AAC
        else -> throw IOException("MimeType is not supported: $this")
    }

    companion object {
        fun fromMimeType(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> PCM
            MediaFormat.MIMETYPE_AUDIO_G711_ALAW -> G711_ALAW
            MediaFormat.MIMETYPE_AUDIO_G711_MLAW -> G711_MLAW
            MediaFormat.MIMETYPE_AUDIO_AAC -> AAC
            else -> throw IOException("MimeType is not supported: $mimeType")
        }
    }
}

enum class SoundRate(val value: Int) {
    F_5500HZ(0),
    F_11025HZ(1),
    F_22050HZ(2),
    F_44100HZ(3);

    fun toSampleRate() = when (this) {
        F_5500HZ -> 5500
        F_11025HZ -> 11025
        F_22050HZ -> 22050
        F_44100HZ -> 44100
    }

    companion object {
        fun fromSampleRate(sampleRate: Int) = when (sampleRate) {
            5500 -> F_5500HZ
            11025 -> F_11025HZ
            22050 -> F_22050HZ
            44100 -> F_44100HZ
            else -> throw IOException("Sample rate is not supported: $sampleRate")
        }
    }
}

enum class SoundSize(val value: Int) {
    S_8BITS(0),
    S_16BITS(1);

    fun toByteFormat() = when (this) {
        S_8BITS -> AudioFormat.ENCODING_PCM_8BIT
        S_16BITS -> AudioFormat.ENCODING_PCM_16BIT
    }

    companion object {
        fun fromByteFormat(byteFormat: Int) = when (byteFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> S_8BITS
            AudioFormat.ENCODING_PCM_16BIT -> S_16BITS
            else -> throw IOException("Byte format is not supported: $byteFormat")
        }
    }
}

enum class SoundType(val value: Int) {
    MONO(0),
    STEREO(1);

    fun toChannelConfig() = when (this) {
        MONO -> AudioFormat.CHANNEL_IN_MONO
        STEREO -> AudioFormat.CHANNEL_IN_STEREO
    }

    companion object {
        fun fromChannelConfig(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> MONO
            AudioFormat.CHANNEL_IN_STEREO -> STEREO
            else -> throw IOException("Channel config is not supported: $channelConfig")
        }
    }
}

enum class AACPacketType(val value: Int) {
    SEQUENCE_HEADER(0),
    RAW(1)
}