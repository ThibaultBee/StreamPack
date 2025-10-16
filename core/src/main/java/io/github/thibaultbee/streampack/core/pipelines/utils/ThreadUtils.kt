/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2024 Thibault B.
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

import android.os.Build
import android.os.Process
import io.github.thibaultbee.streampack.core.elements.utils.ProcessThreadPriorityValue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread related utils.
 */
internal object ThreadUtils {
    internal const val THREAD_NAME_PREFIX = "StreamPack-"
    internal const val THREAD_NAME_AUDIO_PREFIX = THREAD_NAME_PREFIX + "audio-"
    internal const val THREAD_NAME_VIDEO_PREFIX = THREAD_NAME_PREFIX + "video-"

    /**
     * Pulled from kNiceValues in AOSP.
     *
     * This is a map of Java thread priorities which range from 1 to 10 (10 being highest priority)
     * Android thread priorities, which range from about 20 to -20 (-20 being highest priority).
     *
     * For this thread priority mapping:
     * - 1 is mapped to 19 (lowest)
     * - 5 is mapped to 0 (default)
     * - 10 is mapped to -8 (urgent display)
     */
    private val NICE_VALUES =
        intArrayOf(
            Process.THREAD_PRIORITY_LOWEST, // 1 (Thread.MIN_PRIORITY)
            Process.THREAD_PRIORITY_BACKGROUND + 6,
            Process.THREAD_PRIORITY_BACKGROUND + 3,
            Process.THREAD_PRIORITY_BACKGROUND,
            Process.THREAD_PRIORITY_DEFAULT, // 5 (Thread.NORM_PRIORITY)
            Process.THREAD_PRIORITY_DEFAULT - 2,
            Process.THREAD_PRIORITY_DEFAULT - 4,
            Process.THREAD_PRIORITY_URGENT_DISPLAY + 3,
            Process.THREAD_PRIORITY_URGENT_DISPLAY + 2,
            Process.THREAD_PRIORITY_URGENT_DISPLAY, // 10 (Thread.MAX_PRIORITY)
        )

    internal fun processToJavaPriority(@ProcessThreadPriorityValue processPriority: Int): Int {
        // Err on the side of increased priority.
        for (i in NICE_VALUES.indices) {
            if (processPriority >= NICE_VALUES[i]) {
                return i + 1
            }
        }
        return Thread.MAX_PRIORITY
    }

    /**
     * Creates a fixed thread pool with threads having the given priority and a name starting with
     * [baseName].
     *
     * Threads will be named as `baseName0`, `baseName1`...
     *
     * @param nThread Number of threads in the pool.
     * @param baseName Base name for threads.
     * @param priority Thread priority for each thread from [Android.os.Process] thread priorities.
     * @return The created [ExecutorService].
     */
    internal fun newFixedThreadPool(
        nThread: Int,
        baseName: String,
        @ProcessThreadPriorityValue priority: Int
    ): ExecutorService {
        /* val t ThreadFactory { runnable ->
             val javaPriority = androidToJavaPriority(androidPriority)
             val thread: Thread =
                 this.newThread {
                     // Set the Android thread priority once the thread actually starts running.
                     Process.setThreadPriority(androidPriority)
                     runnable.run()
                 }
 
             // Setting java priority internally sets the android priority, but not vice versa.
             // By setting the java priority here, we ensure that the priority is set to the same or
             // higher priority when the thread starts so that it is scheduled quickly. When the
             // runnable executes, the Android priority, which is more fine grained, is set before
             // the wrapped runnable executes.
             thread.priority = javaPriority
             thread
         }*/

        val threadFactory = object : ThreadFactory {
            private val counter = AtomicInteger(0)

            override fun newThread(runnable: Runnable): Thread {
                val thread = object : Thread(baseName + counter.getAndIncrement()) {
                    override fun run() {
                        Process.setThreadPriority(priority)
                        runnable.run()
                    }
                }
                 
                // Setting java priority internally sets the android priority, but not vice versa.
                // By setting the java priority here, we ensure that the priority is set to the same or
                // higher priority when the thread starts so that it is scheduled quickly. When the
                // runnable executes, the Android priority, which is more fine grained, is set before
                // the wrapped runnable executes.
                thread.priority = processToJavaPriority(priority)
                return thread
            }
        }

        return Executors.newFixedThreadPool(nThread, threadFactory)
    }

    internal val defaultVideoPriorityValue =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Process.THREAD_PRIORITY_VIDEO
        } else {
            Process.THREAD_PRIORITY_DEFAULT
        }
}