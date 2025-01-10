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
package io.github.thibaultbee.streampack.core.elements.processing.video.source

import android.util.Size
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue

/**
 * Interface to get the orientation of the capture surface.
 * These information are used to rotate the frames in the codec surface if the source needs to be rotated.
 * It might not be the case for certain sources.
 */
interface ISourceInfoProvider {
    /**
     * Orientation in degrees of the source.
     * For camera, it is the sensor orientation.
     * To follow device rotation use, it is the current device rotation.
     * Expected values: 0, 90, 180, 270.
     */
    @get:IntRange(from = 0, to = 359)
    val rotationDegrees: Int

    /**
     * True if the source is natively mirrored.
     */
    val isMirror: Boolean

    /**
     * Calculates the relative rotation between the source and the destination.
     *
     * @param targetRotation rotation of the destination (Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180 or Surface.ROTATION_270).
     * @param requiredMirroring true if the destination requires mirroring.
     * @return relative rotation between the source and the destination.
     */
    @IntRange(from = 0, to = 359)
    fun getRelativeRotationDegrees(
        @RotationValue targetRotation: Int,
        requiredMirroring: Boolean
    ): Int = 0

    /**
     * Gets the size of the surface to allocate to display the source.
     */
    fun getSurfaceSize(size: Size, @RotationValue targetRotation: Int): Size = size

    /**
     * Adds a listener to be notified when the orientation changes.
     *
     * @param listener to add.
     */
    fun addListener(listener: ISourceInfoListener)

    /**
     * Removes a listener.
     *
     * @param listener to remove.
     */
    fun removeListener(listener: ISourceInfoListener)

    /**
     * Removes all registered listeners.
     */
    fun removeAllListeners()
}

/**
 * Interface to be notified when the orientation changes.
 */
interface ISourceInfoListener {
    /**
     * Called when the orientation changes.
     */
    fun onInfoChanged()
}