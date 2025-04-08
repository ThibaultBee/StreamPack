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
package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Rect
import android.util.Size

object ViewPortUtils {
    /**
     * Calculates the viewport rectangle based on the aspect ratio mode and source/target sizes.
     */
    fun calculateViewportRect(
        aspectRatioMode: AspectRatioMode, sourceSize: Size, targetSize: Size
    ): Rect {
        val sourceWidth = sourceSize.width
        val sourceHeight = sourceSize.height
        val targetWidth = targetSize.width
        val targetHeight = targetSize.height

        return calculateViewportRect(
            aspectRatioMode,
            sourceWidth,
            sourceHeight,
            targetWidth,
            targetHeight
        )
    }

    /**
     * Calculates the viewport rectangle based on the aspect ratio mode and source/target dimensions.
     */
    fun calculateViewportRect(
        aspectRatioMode: AspectRatioMode, sourceWidth: Int, sourceHeight: Int,
        targetWidth: Int, targetHeight: Int
    ): Rect {
        val sourceRatio = sourceWidth.toFloat() / sourceHeight
        val targetRatio = targetWidth.toFloat() / targetHeight

        return when (aspectRatioMode) {
            AspectRatioMode.STRETCH -> {
                // Use full target size
                Rect(0, 0, targetWidth, targetHeight)
            }

            AspectRatioMode.PRESERVE -> {
                if (sourceRatio > targetRatio) {
                    // Letterbox (black bars on top and bottom)
                    val newHeight = (targetWidth / sourceRatio).toInt()
                    val yOffset = (targetHeight - newHeight) / 2
                    Rect(0, yOffset, targetWidth, yOffset + newHeight)
                } else {
                    // Pillarbox (black bars on sides)
                    val newWidth = (targetHeight * sourceRatio).toInt()
                    val xOffset = (targetWidth - newWidth) / 2
                    Rect(xOffset, 0, xOffset + newWidth, targetHeight)
                }
            }

            AspectRatioMode.CROP -> {
                if (sourceRatio > targetRatio) {
                    // Crop sides
                    val newWidth = (targetHeight * sourceRatio).toInt()
                    val xOffset = (targetWidth - newWidth) / 2
                    Rect(xOffset, 0, xOffset + newWidth, targetHeight)
                } else {
                    // Crop top/bottom
                    val newHeight = (targetWidth / sourceRatio).toInt()
                    val yOffset = (targetHeight - newHeight) / 2
                    Rect(0, yOffset, targetWidth, yOffset + newHeight)
                }
            }
        }
    }
}

enum class AspectRatioMode {
    /**
     * Maintain aspect ratio by adding letterbox/pillarbox as needed
     */
    PRESERVE,

    /**
     * Stretch content to fill target surface completely
     */
    STRETCH,

    /**
     * Crop content to fill target while maintaining aspect ratio
     */
    CROP
}