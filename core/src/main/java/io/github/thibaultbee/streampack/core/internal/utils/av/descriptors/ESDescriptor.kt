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
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putString
import java.nio.ByteBuffer

class ESDescriptor(
    private val esId: Short,
    private val streamPriority: Byte,
    private val dependsOnEsId: Short? = null,
    private val url: String? = null,
    private val ocrEsId: Short? = null,
    private val decoderConfigDescriptor: DecoderConfigDescriptor,
    private val slConfigDescriptor: SLConfigDescriptor
    // TODO: other descriptor
) : BaseDescriptor(Tags.ESDescr, 3 + (dependsOnEsId?.let { 2 } ?: 0) + (url?.let { 1 + it.length }
    ?: 0) + (ocrEsId?.let { 2 } ?: 0) + decoderConfigDescriptor.size + slConfigDescriptor.size) {

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putShort(esId)
        output.put(((dependsOnEsId?.let { 1 }
            ?: 0) shl 7) or ((url?.let { 1 }
            ?: 0) shl 6) or ((url?.let { 1 }
            ?: 0) shl 5) or streamPriority.toInt())
        dependsOnEsId?.let {
            output.putShort(it)
        }
        url?.let {
            output.put(it.length)
            output.putString(it)
        }
        ocrEsId?.let {
            output.putShort(it)
        }
        decoderConfigDescriptor.write(output)
        slConfigDescriptor.write(output)
    }
}