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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import java.nio.ByteBuffer

class SampleTableBox(
    private val stsd: SampleDescriptionBox,
    private val stts: TimeToSampleBox,
    private val stss: SyncSampleBox? = null,
    private val stsc: SampleToChunkBox,
    private val stsz: SampleSizeBox,
    private val co: BaseChunkOffsetBox
) : Box("stbl") {
    override val size: Int =
        super.size + stsd.size + stts.size + (stss?.size ?: 0) + stsc.size + stsz.size + co.size

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        stsd.write(buffer)
        stts.write(buffer)
        stss?.write(buffer)
        stsc.write(buffer)
        stsz.write(buffer)
        co.write(buffer)
    }
}