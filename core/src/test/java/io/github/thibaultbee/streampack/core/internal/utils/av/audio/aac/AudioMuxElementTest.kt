/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac

import android.media.AudioFormat
import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.AudioSpecificConfig
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.core.internal.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AudioMuxElementTest {
    @Test
    fun `test StreamMuxConfig`() {
        // Only first 44 bits
        val expectedAudioMuxElement = byteArrayOf(0x40, 0x00, 0x24, 0x10, 0x3F, 0xC0.toByte())

        val config = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )

        val decoderSpecificInfo = AudioSpecificConfig.fromAudioConfig(config).toByteBuffer()
        val streamMuxConfig = StreamMuxConfig.fromDecoderSpecificInfo(decoderSpecificInfo)
        assertArrayEquals(
            expectedAudioMuxElement,
            streamMuxConfig.toByteBuffer().toByteArray()
        )
    }

    @Test
    fun `test AudioMuxElement`() {
        val expectedAudioMuxElement =
            ResourcesUtils.readByteBuffer("test-samples/audio/latm/aac-lc-44100Hz-mono/audio-mux-element")

        val payload =
            ResourcesUtils.readByteBuffer("test-samples/audio/latm/aac-lc-44100Hz-mono/frame.raw")

        val config = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
        )

        val decoderSpecificInfo = AudioSpecificConfig.fromAudioConfig(config).toByteBuffer()
        val audioMuxElement = AudioMuxElement.fromDecoderSpecificInfo(payload, decoderSpecificInfo)
        assertArrayEquals(
            expectedAudioMuxElement.array(),
            audioMuxElement.toByteBuffer().toByteArray()
        )
    }
}