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
package io.github.thibaultbee.streampack.core.elements.processing.audio

import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

open class FrameProcessor(val onFrame: (Frame) -> Unit) {
    private val processExecutor = Executors.newSingleThreadExecutor()
    private val listenerExecutor = Executors.newSingleThreadExecutor()
    private val frameExecutor = Executors.newSingleThreadExecutor()

    private var getFrame: ((inputBuffer: ByteBuffer?) -> Frame)? = null

    private val isRunning = AtomicBoolean(false)

    fun setInput(getFrame: (inputBuffer: ByteBuffer?) -> Frame) {
        listenerExecutor.execute {
            this.getFrame = getFrame
        }
    }

    fun removeInput() {
        listenerExecutor.execute {
            this.getFrame = null
        }
    }

    /**
     * Process frame.
     */
    open fun processFrame(frame: Frame) = frame

    fun startStream() {
        if (isRunning.getAndSet(true)) {
            Logger.w(TAG, "Stream is already running")
            return
        }
        processExecutor.execute {
            while (isRunning.get()) {
                val frameFuture = listenerExecutor.submit<Frame?> {
                    val listener = getFrame ?: return@submit null
                    try {
                        listener(null)
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Failed to get audio frame: ${t.message}")
                        null
                    }
                }
                val frame = frameFuture.get() ?: continue

                // Process buffer with effects
                processFrame(frame)

                // Store for outputs
                frameExecutor.execute {
                    onFrame(frame)
                }
            }
        }
    }

    fun stopStream() {
        if (!isRunning.getAndSet(false)) {
            Logger.w(TAG, "Stream is already stopped")
            return
        }
    }

    fun release() {
        stopStream()
        processExecutor.shutdown()
    }

    companion object {
        private const val TAG = "FrameProcessor"
    }
}