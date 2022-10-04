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
package io.github.thibaultbee.streampack.internal.utils.av.video

import java.nio.ByteBuffer

fun ByteBuffer.getStartCodeSize(): Int {
    return if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
        && this.get(2) == 0x00.toByte() && this.get(3) == 0x01.toByte()
    ) {
        4
    } else if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
        && this.get(2) == 0x01.toByte()
    ) {
        3
    } else {
        0
    }
}

fun ByteBuffer.removeStartCode(): ByteBuffer {
    val startCodeSize = this.getStartCodeSize()
    this.position(startCodeSize)
    return this.slice()
}

enum class ChromaFormat(val value: Byte) {
    YUV400(0),
    YUV420(1),
    YUV422(2),
    YUV444(3);

    companion object {
        fun fromChromaIdc(chromaIdc: Byte) =
            values().first { it.value == chromaIdc }
    }
}
