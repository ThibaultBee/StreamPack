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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.opus

import io.github.thibaultbee.streampack.core.internal.utils.extensions.getLong
import io.github.thibaultbee.streampack.core.internal.utils.extensions.getString
import io.github.thibaultbee.streampack.core.internal.utils.extensions.startWith
import java.nio.ByteBuffer

class OpusCsdParser {
    companion object {
        private const val MARKER_PREFIX = "AOPUS"

        private const val MARKER_SIZE = 8

        private const val HEADER_MARKER = "${MARKER_PREFIX}HDR"
        private const val CODEC_DELAY_MARKER = "${MARKER_PREFIX}DLY"
        private const val SEEK_PREROLL_MARKER = "${MARKER_PREFIX}PRL"

        private const val LENGTH_SIZE = Long.SIZE_BYTES

        fun isCsdSyntax(buffer: ByteBuffer) = buffer.startWith(MARKER_PREFIX)

        fun parse(buffer: ByteBuffer): Triple<IdentificationHeader, ByteBuffer?, ByteBuffer?> {
            if (IdentificationHeader.isIdentificationHeader(buffer)) {
                return Triple(IdentificationHeader.parse(buffer), null, null)
            }

            var identificationHeader: IdentificationHeader? = null
            while (buffer.remaining() >= (MARKER_SIZE + LENGTH_SIZE)) {
                val marker = buffer.getString(MARKER_SIZE)
                val length = buffer.getLong(true)
                val position = buffer.position()
                when (marker) {
                    HEADER_MARKER -> {
                        identificationHeader = IdentificationHeader.parse(buffer)
                    }

                    CODEC_DELAY_MARKER -> {
                        // TODO
                    }

                    SEEK_PREROLL_MARKER -> {
                        // TODO
                    }

                    else -> throw IllegalArgumentException("Unknown Opus marker: $marker")
                }
                buffer.position(position + length.toInt())
            }

            if (identificationHeader == null) {
                throw IllegalArgumentException("Opus identification header not found")
            }
            return Triple(identificationHeader, null, null)
        }
    }
}