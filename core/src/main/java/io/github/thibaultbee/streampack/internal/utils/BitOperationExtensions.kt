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
package io.github.thibaultbee.streampack.internal.utils

/**
 * Convert a Boolean to an Int.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toInt() = if (this) 1 else 0

/**
 * Convert a Boolean to a Byte.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toByte(): Byte = if (this) 1 else 0

infix fun Boolean.shl(i: Int): Int {
    return this.toInt() shl i
}

infix fun Byte.shl(i: Int): Int {
    return this.toInt() shl i
}

infix fun Byte.shr(i: Int): Int {
    return this.toInt() shr i
}
