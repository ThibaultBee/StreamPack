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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.DecoderConfigDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.DecoderSpecificInfo
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.ESDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.ObjectTypeIndication
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.SLConfigDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.StreamType
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class MP4AudioSampleEntryTest {
    @Test
    fun `write valid mp4a test`() {
        val decoderSpecificInfo =
            ByteBuffer.wrap(byteArrayOf(0x11, 0x90.toByte(), 0x56, 0xE5.toByte(), 0))
        val esDescriptor = ESDescriptor(
            esId = 0,
            streamPriority = 0,
            dependsOnEsId = null,
            url = null,
            ocrEsId = null,
            decoderConfigDescriptor = DecoderConfigDescriptor(
                objectTypeIndication = ObjectTypeIndication.AUDIO_ISO_14496_3_AAC,
                streamType = StreamType.AudioStream,
                upStream = false,
                bufferSize = 478,
                maxBitrate = 139520,
                avgBitrate = 95928,
                decoderSpecificInfo = DecoderSpecificInfo(decoderSpecificInfo)
            ),
            slConfigDescriptor = SLConfigDescriptor(predefined = 2)
        )

        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("mp4a.box")
        val mp4a = MP4AudioSampleEntry(
            channelCount = 2,
            sampleSize = 16,
            sampleRate = 48000,
            esds = ESDSBox(esDescriptor)
        )
        val buffer = mp4a.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}