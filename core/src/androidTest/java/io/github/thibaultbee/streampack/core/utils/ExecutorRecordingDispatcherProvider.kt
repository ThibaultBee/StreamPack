/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.utils

import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [IDispatcherProvider] that keeps the executors it creates and counts the tasks they reject,
 * by component name.
 */
class ExecutorRecordingDispatcherProvider(
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider()
) : IDispatcherProvider by dispatcherProvider {
    val executors = CopyOnWriteArrayList<ExecutorService>()
    private val rejections = ConcurrentHashMap<String, AtomicInteger>()

    fun rejections(componentName: String) = rejections[componentName]?.get() ?: 0

    private fun newExecutor(numOfThread: Int, componentName: String): ExecutorService {
        val threadCount = AtomicInteger(0)
        return ThreadPoolExecutor(
            numOfThread,
            numOfThread,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            { runnable -> Thread(runnable, "test-$componentName${threadCount.getAndIncrement()}") },
            { _, _ ->
                rejections.getOrPut(componentName) { AtomicInteger(0) }.incrementAndGet()
                throw RejectedExecutionException("$componentName is shut down")
            }
        ).also { executors.add(it) }
    }

    override fun createAudioDispatcher(numOfThread: Int, componentName: String) =
        newExecutor(numOfThread, componentName).asCoroutineDispatcher()

    override fun createVideoDispatcher(numOfThread: Int, componentName: String) =
        newExecutor(numOfThread, componentName).asCoroutineDispatcher()

    override fun createVideoExecutor(numOfThread: Int, componentName: String) =
        newExecutor(numOfThread, componentName)
}
