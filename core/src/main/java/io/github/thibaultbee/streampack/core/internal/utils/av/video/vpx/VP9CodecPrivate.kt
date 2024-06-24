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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

import java.nio.ByteBuffer
import kotlin.experimental.and

// NOT TESTED
data class VP9CodecPrivate(
    val features: List<CodecFeature>
) {
    fun getFeature(id: CodecFeature.Ids) = features.first { it.id == id.value }

    companion object {
        fun parse(buffer: ByteBuffer): VP9CodecPrivate {
            val features = mutableListOf<CodecFeature>()
            while (buffer.remaining() > 0) {
                features.add(CodecFeature.parse(buffer))
            }
            return VP9CodecPrivate(features)
        }
    }

    class CodecFeature(
        val id: Byte,
        val length: Byte,
        val data: Any
    ) {
        companion object {
            fun parse(buffer: ByteBuffer): CodecFeature {
                val id = buffer.get() and 0x7F
                val length = buffer.get()
                val data = when (length) {
                    1.toByte() -> buffer.get()
                    2.toByte() -> buffer.short
                    4.toByte() -> buffer.int
                    else -> {
                        val data = ByteArray(length.toInt())
                        buffer.get(data)
                        data
                    }
                }
                return CodecFeature(id, length, data)
            }
        }

        enum class Ids(val value: Byte) {
            PROFILE(0),
            LEVEL(1),
            BIT_DEPTH(2),
            CHROMA_SUBSAMPLING(3);

            companion object {
                fun fromId(id: Byte) = entries.first { it.value == id }
            }
        }
    }
}