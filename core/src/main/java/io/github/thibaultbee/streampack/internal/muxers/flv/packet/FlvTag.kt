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
package io.github.thibaultbee.streampack.internal.muxers.flv.packet

import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.utils.put
import io.github.thibaultbee.streampack.internal.utils.putInt24
import io.github.thibaultbee.streampack.internal.utils.shl
import io.github.thibaultbee.streampack.internal.utils.isAudio
import io.github.thibaultbee.streampack.internal.utils.isVideo
import java.io.IOException
import java.nio.ByteBuffer

abstract class FlvTag(
    private var ts: Long,
    private val type: TagType,
    private val isEncrypted: Boolean = false /* Not supported yet */
) {
    protected abstract fun writeTagHeader(buffer: ByteBuffer)
    protected abstract val tagHeaderSize: Int
    protected abstract fun writePayload(buffer: ByteBuffer)
    protected abstract val payloadSize: Int

    fun write(): ByteBuffer {
        val dataSize = tagHeaderSize + payloadSize
        val flvTagSize = FLV_HEADER_TAG_SIZE + dataSize
        val buffer =
            ByteBuffer.allocateDirect(flvTagSize + 4) // 4 - PreviousTagSize

        // FLV Tag
        buffer.put((isEncrypted shl 5) or (type.value))
        buffer.putInt24(dataSize)
        val tsInMs = (ts / 1000).toInt() // to ms
        buffer.putInt24(tsInMs)
        buffer.put((tsInMs shr 24).toByte())
        buffer.putInt24(0) // Stream ID

        writeTagHeader(buffer)

        if (isEncrypted) {
            throw NotImplementedError("Filter/encryption is not implemented yet")
            // EncryptionTagHeader
            // FilterParams
        }

        writePayload(buffer)

        buffer.putInt(flvTagSize)

        buffer.rewind()

        return buffer
    }

    companion object {
        private const val FLV_HEADER_TAG_SIZE = 11

        fun createFlvTag(frame: Frame, isSequenceHeader: Boolean, config: Config): FlvTag {
            return when {
                frame.mimeType.isAudio() -> if (isSequenceHeader) {
                    AudioTag(frame.pts, frame.extra!![0], true, config as AudioConfig)
                } else {
                    AudioTag(frame.pts, frame.buffer, false, config as AudioConfig)
                }
                frame.mimeType.isVideo() -> if (isSequenceHeader) {
                    VideoTag(
                        frame.pts,
                        frame.extra!!,
                        frame.isKeyFrame,
                        true,
                        config as VideoConfig
                    )
                } else {
                    VideoTag(
                        frame.pts,
                        frame.buffer,
                        frame.isKeyFrame,
                        false,
                        config as VideoConfig
                    )
                }
                else -> {
                    throw IOException("Frame is neither video nor audio: ${frame.mimeType}")
                }
            }
        }
    }
}

enum class TagType(val value: Int) {
    AUDIO(8),
    VIDEO(9),
    SCRIPT(18),
}

