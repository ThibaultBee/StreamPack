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

import io.github.thibaultbee.streampack.internal.utils.av.video.av1.AV1CodecConfigurationRecord
import java.nio.ByteBuffer

sealed class AV1CodecConfigurationBox : Box("av1C")

class AV1CodecConfigurationBox1(private val config: AV1CodecConfigurationRecord) :
    AV1CodecConfigurationBox() {
    override val size: Int = super.size + config.size

    override fun write(output: ByteBuffer) {
        super.write(output)
        config.write(output)
    }
}

class AV1CodecConfigurationBox2(private val buffer: ByteBuffer) : AV1CodecConfigurationBox() {
    override val size: Int = super.size + buffer.remaining()

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(buffer)
    }
}