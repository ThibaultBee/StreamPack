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
package io.github.thibaultbee.streampack.core.elements.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutineScheduler(
    private val delayTimeInMs: Long,
    coroutineDispatcher: CoroutineDispatcher,
    private val action: suspend CoroutineScope.() -> Unit
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatcher)
    private var job: Job? = null

    fun start() {
        if (job != null) {
            return
        }
        job = coroutineScope.launch {
            while (true) {
                delay(delayTimeInMs)
                launch { action() }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        coroutineScope.cancel()
    }
}
