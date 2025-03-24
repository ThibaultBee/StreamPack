/*
 * Copyright 2025 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils.pool

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.logger.Logger

/**
 * A factory to create [RawFrame].
 */
fun RawFrameFactory(isDirect: Boolean): RawFrameFactory {
    return RawFrameFactory(ByteBufferPool(isDirect))
}

/**
 * A factory to create [RawFrame].
 */
class RawFrameFactory(private val bufferPool: ByteBufferPool) : IRawFrameFactory {
    override fun create(bufferSize: Int, timestampInUs: Long): RawFrame {
        return RawFrame(bufferPool.get(bufferSize), timestampInUs) { rawFrame ->
            try {
                bufferPool.put(rawFrame.rawBuffer)
            } catch (e: Exception) {
                Logger.w(TAG, "Error while putting buffer in pool", e)
            }
        }
    }

    override fun clear() {
        bufferPool.clear()
    }

    override fun close() {
        bufferPool.close()
    }

    companion object {
        private const val TAG = "RawFramePool"
    }
}