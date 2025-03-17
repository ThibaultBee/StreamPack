/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.runningFold

data class History<T>(val previous: T?, val current: T)

fun <T> SharedFlow<T>.runningHistoryNotNull(): Flow<History<T>> =
    runningHistory().filterNotNull()

fun <T> SharedFlow<T>.runningHistory(): Flow<History<T>?> =
    runningFold(
        initial = null as (History<T>?),
        operation = { accumulator, new -> History(accumulator?.current, new) }
    )
