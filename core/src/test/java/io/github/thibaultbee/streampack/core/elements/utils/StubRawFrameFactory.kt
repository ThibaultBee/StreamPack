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
package io.github.thibaultbee.streampack.core.elements.utils

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.utils.pool.IGetOnlyBufferPool
import io.github.thibaultbee.streampack.core.elements.utils.pool.IReadOnlyRawFrameFactory
import java.nio.ByteBuffer

/**
 * Stub buffer pool for testing.
 *
 * It always returns a new allocated buffer.
 */
class StubRawFrameFactory(private val bufferPool: IGetOnlyBufferPool<ByteBuffer> = StubBufferPool()) :
    IReadOnlyRawFrameFactory {
    override fun create(bufferSize: Int, timestampInUs: Long): RawFrame {
        return RawFrame(bufferPool.get(bufferSize), timestampInUs) { rawFrame ->
            // Do nothing
        }
    }
}