/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.interfaces

interface Streamable {
    /**
     * Starts frames or data stream generation
     * Throws an exception if not ready for live stream
     */
    fun startStream()

    /**
     * Stops frames or data stream generation
     */
    fun stopStream()
}

/**
 * Same as [Streamable] but with suspend functions.
 */
interface SuspendStreamable {
    /**
     * Starts frames or data stream generation
     * Throws an exception if not ready for live stream
     */
    suspend fun startStream()

    /**
     * Stops frames or data stream generation
     */
    suspend fun stopStream()
}

interface SuspendCloseable {
    /**
     * Closes and releases resources
     */
    suspend fun close()
}

interface Configurable<T> {
    /**
     * Configure the [Configurable] implementation.
     *
     * @param config [Configurable] implementation configuration
     */
    fun configure(config: T)
}

interface Releasable {
    /**
     * Closes and releases resources
     */
    fun release()
}