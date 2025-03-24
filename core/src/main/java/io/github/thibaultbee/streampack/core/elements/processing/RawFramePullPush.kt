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
import io.github.thibaultbee.streampack.core.elements.utils.pool.IRawFrameFactory
import io.github.thibaultbee.streampack.core.elements.utils.pool.RawFrameFactory
import io.github.thibaultbee.streampack.core.logger.Logger
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

fun RawFramePullPush(
    frameProcessor: IFrameProcessor<RawFrame>,
    onFrame: (RawFrame) -> Unit,
    isDirect: Boolean = true
) = RawFramePullPush(frameProcessor, onFrame, RawFrameFactory(isDirect))

/**
 * A component that pull a frame from an input and push it to [onFrame] output.
 *
 * @param frameProcessor the frame processor
 * @param onFrame the output frame callback
 * @param byteBufferPool the buffer pool to get a buffer from
 */
class RawFramePullPush(
    private val frameProcessor: IFrameProcessor<RawFrame>,
    val onFrame: (RawFrame) -> Unit,
    private val frameFactory: IRawFrameFactory
) {
    private val processExecutor = Executors.newSingleThreadExecutor()
    private val frameExecutor = Executors.newSingleThreadExecutor()

    private var getFrame: ((frameFactory: IRawFrameFactory) -> RawFrame)? = null

    private val isRunning = AtomicBoolean(false)
    private var executorTask: Future<*>? = null

    fun setInput(getFrame: (frameFactory: IRawFrameFactory) -> RawFrame) {
        synchronized(this) {
            this.getFrame = getFrame
        }
    }

    fun removeInput() {
        synchronized(this) {
            this.getFrame = null
        }
    }

    fun startStream() {
        if (isRunning.getAndSet(true)) {
            Logger.w(TAG, "Stream is already running")
            return
        }
        executorTask = processExecutor.submit {
            while (isRunning.get()) {
                val rawFrame = synchronized(this) {
                    val listener = getFrame ?: return@synchronized null
                    try {
                        listener(frameFactory)
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Failed to get frame: ${t.message}")
                        null
                    }
                }
                if (rawFrame == null) {
                    continue
                }

                // Process buffer with effects
                val processedFrame = frameProcessor.processFrame(rawFrame)

                // Store for outputs
                frameExecutor.execute {
                    onFrame(processedFrame)
                }
            }
        }
    }

    fun stopStream() {
        if (!isRunning.getAndSet(false)) {
            Logger.w(TAG, "Stream is already stopped")
            return
        }
        executorTask?.get()
        executorTask = null
        frameFactory.clear()
    }

    fun release() {
        stopStream()

        processExecutor.shutdown()
        frameFactory.close()
    }

    companion object {
        private const val TAG = "FrameProcessor"
    }
}