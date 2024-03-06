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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.putInt
import java.nio.ByteBuffer

class TrackFragmentHeaderBox(
    private val id: Int,
    private val baseDataOffset: Long? = null,
    private val sampleDescriptionIndex: Int? = null,
    private val defaultSampleDuration: Int? = null,
    private val defaultSampleSize: Int? = null,
    private val defaultSampleFlags: SampleFlags? = null,
    durationIsEmpty: Boolean = false
) :
    FullBox(
        "tfhd",
        0,
        createFlags(
            baseDataOffset,
            sampleDescriptionIndex,
            defaultSampleDuration,
            defaultSampleSize,
            defaultSampleFlags,
            durationIsEmpty
        )
    ) {
    override val size: Int =
        super.size + 4 + (baseDataOffset?.let { 8 } ?: 0) + (sampleDescriptionIndex?.let { 4 }
            ?: 0) + (defaultSampleDuration?.let { 4 } ?: 0) + (defaultSampleSize?.let { 4 }
            ?: 0) + (defaultSampleFlags?.let { 4 } ?: 0)

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(id)
        baseDataOffset?.let { output.putLong(it) }
        sampleDescriptionIndex?.let { output.putInt(it) }
        defaultSampleDuration?.let { output.putInt(it) }
        defaultSampleSize?.let { output.putInt(it) }
        defaultSampleFlags?.let { output.putInt(it) }
    }

    enum class TrackFragmentFlag(val value: Int) {
        BASE_DATA_OFFSET_PRESENT(0x000001),
        SAMPLE_DESCRIPTION_INDEX_PRESENT(0x000002),
        DEFAULT_SAMPLE_DURATION_PRESENT(0x000008),
        DEFAULT_SAMPLE_SIZE_PRESENT(0x000010),
        DEFAULT_SAMPLE_FLAGS_PRESENT(0x000020),
        DURATION_IS_EMPTY(0x010000),
        DEFAULT_BASE_IS_MOOF(0x020000)
    }

    companion object {
        private fun createFlags(
            baseDataOffset: Long?,
            sampleDescriptionIndex: Int?,
            defaultSampleDuration: Int?,
            defaultSampleSize: Int?,
            defaultSampleFlags: SampleFlags?,
            durationIsEmpty: Boolean
        ): Int {
            var flags = 0
            baseDataOffset?.let { flags += TrackFragmentFlag.BASE_DATA_OFFSET_PRESENT.value }
            sampleDescriptionIndex?.let { flags += TrackFragmentFlag.SAMPLE_DESCRIPTION_INDEX_PRESENT.value }
            defaultSampleDuration?.let { flags += TrackFragmentFlag.DEFAULT_SAMPLE_DURATION_PRESENT.value }
            defaultSampleSize?.let { flags += TrackFragmentFlag.DEFAULT_SAMPLE_SIZE_PRESENT.value }
            defaultSampleFlags?.let { flags += TrackFragmentFlag.DEFAULT_SAMPLE_FLAGS_PRESENT.value }
            if (durationIsEmpty) flags += TrackFragmentFlag.DURATION_IS_EMPTY.value
            return flags
        }
    }
}
