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
package com.github.thibaultbee.streampack.internal.utils

import android.os.Build

/**
 * Get absolute value of Byte
 */
val Byte.absoluteValue: Byte
    get() {
        return if (this > 0) {
            this
        } else {
            (-this).toByte()
        }
    }

/**
 * Get absolute value of Short
 */
val Short.absoluteValue: Short
    get() {
        return if (this > 0) {
            this
        } else {
            (-this).toShort()
        }
    }

fun <T> MutableList<T>.replaceAllBy(value: T) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.replaceAll { value }
    } else {
        (0 until this.size).forEach { this[it] = value }
    }
}