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

import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putInt24
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import java.nio.ByteBuffer

open class DecoderConfigDescriptor(
    private val objectTypeIndication: ObjectTypeIndication,
    private val streamType: StreamType,
    private val upStream: Boolean,
    private val bufferSize: Int,
    private val maxBitrate: Int,
    private val avgBitrate: Int,
    private val decoderSpecificInfo: DecoderSpecificInfo,
    // TODO profileLevelIndicationIndexDescriptor
) : BaseDescriptor(Tags.DecoderConfigDescr, 13 + decoderSpecificInfo.size) {

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(objectTypeIndication.value)
        output.put((streamType.value shl 2) or (upStream shl 1) or 1)
        output.putInt24(bufferSize)
        output.putInt(maxBitrate)
        output.putInt(avgBitrate)
        decoderSpecificInfo.write(output)
    }
}

enum class ObjectTypeIndication(val value: Byte) {
    AUDIO_ISO_14496_3_AAC(0x40),
    AUDIO_ISO_13818_3_MP3(0x69);

    companion object {
        fun fromValue(value: Byte) = entries.first { it.value == value }
    }
}

enum class StreamType(val value: Byte) {
    ObjectDescriptorStream(1),
    ClockReferenceStream(2),
    SceneDescriptionStream(3),
    VisualStream(4),
    AudioStream(5),
    MPEG7Stream(6),
    IPMPStream(7),
    ObjectContentInfoStream(8),
    MPEGJStream(9);

    companion object {
        fun fromValue(value: Byte) = entries.first { it.value == value }
    }
}
