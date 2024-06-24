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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.containers

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.AmfParameter
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.primitives.AmfNamedParameter
import java.nio.ByteBuffer

open class AmfContainer : AmfParameter() {
    protected val parameters = mutableListOf<AmfParameter>()

    fun add(amfParameter: AmfParameter) {
        parameters.add(amfParameter)
    }

    fun add(v: Any) {
        parameters.add(build(v))
    }

    fun add(name: String, v: Any) {
        parameters.add(AmfNamedParameter(name, v))
    }

    override val size: Int
        get() = parameters.sumOf { it.size }

    override fun encode(buffer: ByteBuffer) {
        parameters.forEach { it.encode(buffer) }
    }
}