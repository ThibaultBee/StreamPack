/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.interfaces

import android.graphics.SurfaceTexture
import android.util.Size

/**
 * Interface to get the orientation of the capture surface.
 * These information are used to rotate the frames in the codec surface if the source needs to be rotated.
 * It might not be the case for certain sources.
 */
interface ISourceOrientationProvider {
    /**
     * Orientation in degrees of the surface.
     * Expected values: 0, 90, 180, 270.
     */
    val orientation: Int

    /**
     * Return the size with the correct orientation.
     * If orientation is portrait, it returns a portrait size.
     * Example:
     *  - Size = 1920x1080, if orientation is portrait, it returns 1080x1920.
     */
    fun getOrientedSize(size: Size): Size

    /**
     * Return the size for [SurfaceTexture.setDefaultBufferSize].
     * Override this method if the image is stretched.
     */
    fun getDefaultBufferSize(size: Size) = size
}