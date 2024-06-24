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
package io.github.thibaultbee.streampack.core.internal.utils.av.descriptors

import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.core.internal.utils.ResourcesUtils
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer


class ESDescriptorTest {
    @Test
    fun `write valid ESDescriptor test`() {
        val expectedBuffer =
            ResourcesUtils.readByteBuffer("test-samples/utils/av/descriptors/es.descriptor")
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

        val buffer = esDescriptor.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}
