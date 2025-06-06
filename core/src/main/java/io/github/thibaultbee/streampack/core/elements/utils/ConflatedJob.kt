/*
 * Copyright (c) 2023 DuckDuckGo
 * Copyright (C) 2025 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.thibaultbee.streampack.core.elements.utils

import kotlinx.coroutines.Job

/**
 * Job util class that will cancel the previous job if a new one is set.
 *
 * Assign the new job with the += operator.
 */
class ConflatedJob {

    private var job: Job? = null

    val isActive get() = job?.isActive ?: false

    @Synchronized
    operator fun plusAssign(newJob: Job) {
        cancel()
        job = newJob
    }

    fun cancel() {
        job?.cancel()
    }

    fun start() {
        job?.start()
    }

    suspend fun join() {
        job?.join()
    }
}