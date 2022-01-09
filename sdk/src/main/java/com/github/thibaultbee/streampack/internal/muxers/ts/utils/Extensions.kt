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
package com.github.thibaultbee.streampack.internal.muxers.ts.utils

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Convert a Boolean to an Int.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toInt() = if (this) 1 else 0

infix fun Boolean.shl(i: Int): Int {
    return this.toInt() shl i
}

infix fun Byte.shl(i: Int): Int {
    return this.toInt() shl i
}

fun ByteBuffer.put(i: Int, i1: Int) {
    put(i, i1.toByte())
}

fun ByteBuffer.put(s: Short) {
    put(s.toByte())
}

fun ByteBuffer.put(i: Int) {
    put(i.toByte())
}

fun ByteBuffer.putShort(l: Long) {
    putShort(l.toShort())
}

fun ByteBuffer.putShort(i: Int) {
    putShort(i.toShort())
}

fun ByteBuffer.putString(s: String) {
    for (c in s.toByteArray(StandardCharsets.UTF_8)) {
        put(c)
    }
}