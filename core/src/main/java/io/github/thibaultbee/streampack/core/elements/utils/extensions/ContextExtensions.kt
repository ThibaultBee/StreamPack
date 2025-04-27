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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Rect
import android.util.Size
import android.view.Surface
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.view.DisplayCompat
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.WindowUtils

/**
 * Returns the display orientation in degrees from the natural orientation.
 *
 * @return the display orientation in degrees
 */
val Context.displayRotationDegrees: Int
    @IntRange(
        from = 0,
        to = 359
    )
    get() = displayRotation.rotationToDegrees

/**
 * Returns the display orientation in degrees from the natural orientation.
 *
 * @return the display orientation as [Surface.ROTATION_0], [Surface.ROTATION_90], [Surface.ROTATION_180] or [Surface.ROTATION_270]
 */

val Context.displayRotation: Int
    @RotationValue
    get() {
        return Surface.ROTATION_90
//        return ContextCompat.getDisplayOrDefault(this).rotation
    }

/**
 * Whether the device is in portrait orientation.
 */
val Context.isDevicePortrait: Boolean
    get() = isRotationPortrait(displayRotation)

/**
 * Whether the device is in landscape orientation.
 */
val Context.isDeviceLandscape: Boolean
    get() = !isDevicePortrait

/**
 * Returns the device natural size in pixels.
 */
private val Context.naturalSize: Size
    get() {
        val display = ContextCompat.getDisplayOrDefault(this)
        val mode = DisplayCompat.getMode(this, display)
        return Size(mode.physicalWidth, mode.physicalHeight)
    }

/**
 * Whether the natural orientation is portrait.
 */
val Context.isNaturalToPortrait: Boolean
    get() = naturalSize.isPortrait

/**
 * Whether the application is in landscape orientation.
 */
val Context.isNaturalToLandscape: Boolean
    get() = !isNaturalToPortrait

/**
 * Whether the application is in portrait.
 *
 * @return true if the application is in portrait, otherwise false
 */
val Context.isApplicationPortrait: Boolean
    get() = resources.configuration.orientation == ORIENTATION_PORTRAIT

/**
 * Whether the application is in landscape.
 *
 * @return true if the application is in landscape, otherwise false
 */
val Context.isApplicationLandscape: Boolean
    get() = resources.configuration.orientation == ORIENTATION_LANDSCAPE

/**
 * Whether the rotation is portrait for this device.
 */
fun Context.isRotationDegreesPortrait(
    @IntRange(
        from = 0,
        to = 359
    ) rotationDegrees: Int
): Boolean {
    require(rotationDegrees.is90Multiple) { "Orientation must be a multiple of 90 but $rotationDegrees" }
    return if (isNaturalToPortrait) {
        rotationDegrees % 180 == 0
    } else {
        rotationDegrees % 180 != 0
    }
}

/**
 * Whether the rotation is portrait for this device.
 */
fun Context.isRotationPortrait(
    @RotationValue rotation: Int
): Boolean {
    return if (isNaturalToPortrait) {
        rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    } else {
        rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }
}

/**
 * Get the application pixel density
 */
val Context.densityDpi: Int
    get() = resources.displayMetrics.densityDpi

/**
 * 获取屏幕矩形区域(强制横屏尺寸)
 */
val Context.screenRect: Rect
    get() {
        val rect = WindowUtils.getScreenRect(this)
        return if (rect.width() > rect.height()) rect 
        else Rect(0, 0, rect.height(), rect.width())
    }
