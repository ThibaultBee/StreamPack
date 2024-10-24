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
package io.github.thibaultbee.streampack.core.internal.utils.extensions

import android.media.MediaFormat
import android.util.Size
import java.nio.ByteBuffer

/**
 * Extracts csd buffers from MediaFormat.
 * It can contains SPS, PPS for AVC.
 */
val MediaFormat.extra: List<ByteBuffer>?
    get() {
        if (!containsKey("csd-0") && !containsKey("csd-1") && !containsKey("csd-2")) {
            return null
        }

        val extra = mutableListOf<ByteBuffer>()

        getByteBuffer("csd-0")?.let {
            /**
             * For HEVC, vps, sps amd pps are all in csd-0.
             * They all start with a start code 0x00000001.
             */
            if (getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                val parameterSets = it.slices(byteArrayOf(0x00, 0x00, 0x00, 0x01))
                extra.add(parameterSets[1]) // SPS
                extra.add(parameterSets[0]) // PPS
                extra.add(parameterSets[2]) // VPS
            } else {
                extra.add(it.duplicate())
            }
        }
        getByteBuffer("csd-1")?.let {
            extra.add(it.duplicate())
        }
        getByteBuffer("csd-2")?.let {
            extra.add(it.duplicate())
        }

        return extra
    }

/**
 * Extracts resolution from a [MediaFormat].
 * Only for [MediaFormat] of video.
 */
val MediaFormat.resolution: Size
    get() {
        return Size(
            getInteger(MediaFormat.KEY_WIDTH),
            getInteger(MediaFormat.KEY_HEIGHT)
        )
    }