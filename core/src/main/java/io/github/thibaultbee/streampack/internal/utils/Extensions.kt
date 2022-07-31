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
package io.github.thibaultbee.streampack.internal.utils

fun Any.numOfBits(): Int {
    return when (this) {
        is Byte -> 8
        is Short -> 16
        is Int -> 32
        is Long -> 64
        is Float -> 32
        is Double -> 64
        is Boolean -> 1
        is Char -> 16
        is String -> 8 * this.length
        else -> throw IllegalArgumentException("Unsupported type: ${this.javaClass.name}")
    }
}