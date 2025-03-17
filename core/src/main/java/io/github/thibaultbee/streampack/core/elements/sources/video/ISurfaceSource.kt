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
package io.github.thibaultbee.streampack.core.elements.sources.video

import android.view.Surface

/**
 * Interface for video source that provides a [Surface] for video stream.
 */
interface ISurfaceSourceInternal {
    /**
     * The offset between source capture time and MONOTONIC clock in nanoseconds. It is used to
     * synchronize video with audio. It is only useful for camera source.
     */
    val timestampOffsetInNs: Long

    /**
     * Gets the output surface for the video stream.
     *
     * @return The [Surface] used for video capture
     */
    suspend fun getOutput(): Surface?

    /**
     * Sets the output surface for the video stream.
     *
     * It is called by the [ISurfaceSourceInternal] user.
     *
     * @param surface The [Surface] used for video capture
     */
    suspend fun setOutput(surface: Surface)

    /**
     * Resets the output surface for the video stream.
     *
     * When the output is reset, the [ISurfaceSourceInternal] should stop sending frames to the surface set
     * by [setOutput]. The implementation must forget the previous surface.
     * [getOutput] should return null until a new surface is set.
     */
    suspend fun resetOutput()
}