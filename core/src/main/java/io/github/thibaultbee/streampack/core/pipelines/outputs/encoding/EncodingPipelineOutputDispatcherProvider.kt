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
package io.github.thibaultbee.streampack.core.pipelines.outputs.encoding

import android.os.Process
import io.github.thibaultbee.streampack.core.elements.utils.ProcessThreadPriorityValue
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutputDispatcherProvider.Companion.DEFAULT_NUMBER_OF_AUDIO_THREAD
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutputDispatcherProvider.Companion.DEFAULT_NUMBER_OF_VIDEO_THREAD
import io.github.thibaultbee.streampack.core.pipelines.utils.ThreadUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Creates a [EncodingOutputDispatcherProvider] with the number of audio and video threads and priorities specified.
 *
 * @param defaultDispatcher Default dispatcher for general tasks. A general tasks is a task that is not audio, video or network related.
 * @param ioDispatcher Dispatcher for IO tasks: writing to files, network...
 * @param numOfAudioThreads Number of threads for audio processing threads. Default to 2.
 * @param audioThreadPriority Thread priority for audio processing threads. Default to [Process.THREAD_PRIORITY_AUDIO].
 * @param numOfVideoThreads Number of threads for video processing threads. Default to 2.
 * @param videoThreadPriority Thread priority for video processing threads. Default to [ThreadUtils.defaultVideoPriorityValue].
 */
fun EncodingOutputDispatcherProvider(
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    numOfAudioThreads: Int = DEFAULT_NUMBER_OF_AUDIO_THREAD,
    @ProcessThreadPriorityValue audioThreadPriority: Int = Process.THREAD_PRIORITY_AUDIO,
    numOfVideoThreads: Int = DEFAULT_NUMBER_OF_VIDEO_THREAD,
    @ProcessThreadPriorityValue videoThreadPriority: Int = ThreadUtils.defaultVideoPriorityValue
) = EncodingPipelineOutputDispatcherProvider(
    defaultDispatcher = defaultDispatcher,
    audioDispatcher = ThreadUtils.newFixedThreadPool(
        numOfAudioThreads, ThreadUtils.THREAD_NAME_AUDIO_PREFIX, audioThreadPriority
    ).asCoroutineDispatcher(),
    videoDispatcher = ThreadUtils.newFixedThreadPool(
        numOfVideoThreads, ThreadUtils.THREAD_NAME_VIDEO_PREFIX, videoThreadPriority
    ).asCoroutineDispatcher(),
    ioDispatcher = ioDispatcher
)

/**
 * Provides [CoroutineDispatcher]s for encoding pipelines and its elements (encoders, muxers...).
 */
data class EncodingPipelineOutputDispatcherProvider(
    /**
     * Default dispatcher for general tasks. A general tasks is a task that is not audio, video or network
     * processing related.
     */
    val defaultDispatcher: CoroutineDispatcher,

    /**
     * Dispatcher for audio processing tasks.
     *
     * The audio dispatcher is used for tasks such as encoding, filtering, muxing audio frames...
     * By default, the [audioDispatcher] requires at least 2 threads: one for encoding and one for muxing.
     *
     */
    val audioDispatcher: CoroutineDispatcher,

    /**
     * Dispatcher for video processing tasks.
     *
     * The video dispatcher is used for tasks such as encoding, filtering, muxing video frames...
     * By default, the [videoDispatcher] requires at least 2 threads: one for encoding and one for muxing.
     */
    val videoDispatcher: CoroutineDispatcher,

    /**
     * Dispatcher for IO tasks: writing to files, network...
     */
    val ioDispatcher: CoroutineDispatcher,

    ) {
    companion object Companion {
        internal const val DEFAULT_NUMBER_OF_AUDIO_THREAD = 2
        internal const val DEFAULT_NUMBER_OF_VIDEO_THREAD = 2
    }
}