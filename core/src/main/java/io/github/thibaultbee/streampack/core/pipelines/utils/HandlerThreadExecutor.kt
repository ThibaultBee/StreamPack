/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.pipelines.utils

import android.os.Handler
import android.os.HandlerThread
import io.github.thibaultbee.streampack.core.elements.utils.ProcessThreadPriorityValue
import java.util.concurrent.Executor

/**
 * Helper class that wraps a Handler/HandlerThread combination and implements the [Executor]
 * interface
 */
class HandlerThreadExecutor(name: String, @ProcessThreadPriorityValue priority: Int) :
    Executor {

    private val handlerThread = HandlerThread(name, priority).apply { start() }
    val handler = Handler(handlerThread.looper)

    private var isQuit = false

    fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        handler.postDelayed(runnable, delayMillis)
    }

    fun removeCallbacksAndMessages(token: Any) {
        handler.removeCallbacksAndMessages(token)
    }

    fun removeCallbacks(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }

    override fun execute(runnable: Runnable?) {
        runnable?.let { handler.post(it) }
    }

    fun quit() {
        handlerThread.quit()
        isQuit = true
    }
}