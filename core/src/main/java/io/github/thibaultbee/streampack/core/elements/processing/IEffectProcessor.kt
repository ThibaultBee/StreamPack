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
package io.github.thibaultbee.streampack.core.elements.processing

/**
 * Interface to process a data and returns a result.
 *
 * @param T type of data to proces)
 */
interface IProcessor<T> {
    /**
     * Process a data and returns a result.
     *
     * @param data data to process
     * @return processed data
     */
    fun process(data: T): T
}

/**
 * Interface to process a data and returns a result.
 *
 * @param T type of data to proces)
 */
interface IEffectProcessor<T> {
    /**
     * Process a data and returns a result.
     *
     * @param isMuted whether the data contains only 0
     * @param data data to process
     * @return processed data
     */
    fun process(isMuted: Boolean, data: T): T
}

/**
 * Interface to process a data.
 *
 * @param T type of data to process
 */
interface IEffectConsumer<T> {
    /**
     * Process a data.
     *
     * @param isMuted whether the data contains only 0
     * @param data data to process
     */
    fun consume(isMuted: Boolean, data: T)
}
