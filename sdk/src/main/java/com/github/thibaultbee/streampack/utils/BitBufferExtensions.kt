/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.utils

import com.github.thibaultbee.streampack.muxers.ts.descriptors.AdaptationField
import com.github.thibaultbee.streampack.muxers.ts.packets.PesHeader
import com.github.thibaultbee.streampack.muxers.ts.tables.TableHeader

fun BitBuffer.put(adaptationField: AdaptationField) {
    this.put(adaptationField.toByteBuffer())
}

fun BitBuffer.put(pesHeader: PesHeader) {
    this.put(pesHeader.toByteBuffer())
}

fun BitBuffer.put(tableHeader: TableHeader) {
    this.put(tableHeader.toByteBuffer())
}

/**
 * Write h264 ue
 * @param value to write
 */
fun BitBuffer.putUE(value: Int) {
    var bits = 0
    var cumul = 0
    for (i in 0..14) {
        if (value < cumul + (1 shl i)) {
            bits = i
            break
        }
        cumul += 1 shl i
    }
    put(0, bits)
    put(true)
    put(value - cumul, bits)
}

