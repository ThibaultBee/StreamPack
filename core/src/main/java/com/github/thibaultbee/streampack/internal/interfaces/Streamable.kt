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
package com.github.thibaultbee.streampack.internal.interfaces

interface Streamable<T> {
    /**
     * Configure the [Streamable] implementation.
     *
     * @param config [Streamable] implementation configuration
     */
    fun configure(config: T)

    /**
     * Starts frames or data stream generation
     * Throws an exception if not ready for live stream
     */
    fun startStream()

    /**
     * Stops frames or data stream generation
     */
    fun stopStream()

    /**
     * Closes and releases resources
     */
    fun release()
}