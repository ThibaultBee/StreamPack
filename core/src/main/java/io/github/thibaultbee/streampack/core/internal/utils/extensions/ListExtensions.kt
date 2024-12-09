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
package io.github.thibaultbee.streampack.core.internal.utils.extensions

import android.util.Range

fun <T> Iterable<List<T>>.unzip(): List<List<T>> {
    val expectedSize = this.first().size
    val result = MutableList(expectedSize) { mutableListOf<T>() }
    this.forEach { list ->
        if (list.size != expectedSize) {
            throw IndexOutOfBoundsException("List size must be the same")
        }
        list.forEachIndexed { index, element ->
            result[index].add(element)
        }
    }
    return result
}

fun <T : Comparable<T>> List<Range<T>>.contains(value: T): Boolean {
    return this.any { range -> range.contains(value) }
}