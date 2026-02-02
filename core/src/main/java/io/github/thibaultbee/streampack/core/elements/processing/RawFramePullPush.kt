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
package io.github.thibaultbee.streampack.core.elements.processing

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioFrameSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.pool.ByteBufferPool
import io.github.thibaultbee.streampack.core.elements.utils.pool.RawFramePool
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A component that pull a frame from an input and push it to [onFrame] output.
 *
 * @param frameProcessor the frame processor
 * @param onFrame the output frame callback
 * @param bufferPool the [ByteBuffer] pool
 * @param processDispatcher the dispatcher to process frames on
 */
class RawFramePullPush(
    private val frameProcessor: IProcessor<RawFrame>,
    val onFrame: suspend (RawFrame) -> Unit,
    private val bufferPool: ByteBufferPool,
    private val processDispatcher: CoroutineDispatcher,
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + processDispatcher)
    private val mutex = Mutex()

    private val pool = RawFramePool()

    private var source: IAudioFrameSourceInternal? = null

    private val isReleaseRequested = AtomicBoolean(false)

    private var job: Job? = null

    suspend fun setInput(source: IAudioFrameSourceInternal) {
        mutex.withLock {
            this.source = source
        }
    }

    suspend fun removeInput() {
        mutex.withLock {
            this.source = null
        }
    }

    fun startStream() {
        if (isReleaseRequested.get()) {
            Logger.w(TAG, "Already released")
            return
        }
        job = coroutineScope.launch {
            while (isActive) {
                val rawFrame = mutex.withLock {
                    val unwrapSource = source ?: return@withLock null
                    try {
                        val buffer = bufferPool.get(unwrapSource.minBufferSize)
                        val timestampInUs = unwrapSource.fillAudioFrame(buffer)
                        pool.get(buffer, timestampInUs)
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Failed to get frame: ${t.message}")
                        null
                    }
                }
                if (rawFrame == null) {
                    continue
                }

                // Process buffer with effects
                val processedFrame = try {
                    frameProcessor.process(rawFrame)
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to pre-process frame: ${t.message}")
                    continue
                }

                // Store for outputs
                onFrame(processedFrame)
            }
            Logger.e(TAG, "Processing loop ended")
        }
    }

    fun stopStream() {
        if (isReleaseRequested.get()) {
            Logger.w(TAG, "Already released")
            return
        }
        job?.cancel()
        job = null

        pool.clear()
        bufferPool.clear()
    }

    fun release() {
        stopStream()

        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
            return
        }

        coroutineScope.cancel()
        pool.close()
        bufferPool.close()
    }

    companion object {
        private const val TAG = "FramePullPush"
    }
}