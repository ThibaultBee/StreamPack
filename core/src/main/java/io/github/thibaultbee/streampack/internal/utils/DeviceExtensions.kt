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
package io.github.thibaultbee.streampack.internal.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

/**
 * Returns the device orientation.
 *
 * @return an integer equals to the current device orientation
 */
fun Context.getDeviceOrientation(): Int {
    val displayManager = this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return when (val displayRotation =
        displayManager.getDisplay(Display.DEFAULT_DISPLAY).rotation) {
        Surface.ROTATION_0 -> 90
        Surface.ROTATION_90 -> 0
        Surface.ROTATION_180 -> 270
        Surface.ROTATION_270 -> 180
        else -> throw UnsupportedOperationException(
            "Unsupported display rotation: $displayRotation"
        )
    }
}

/**
 * Check if the device is in portrait.
 *
 * @return true if the device is in portrait, otherwise false
 */
fun Context.isDevicePortrait(): Boolean {
    val orientation = this.getDeviceOrientation()
    return orientation == 90 || orientation == 270
}

/**
 * Check if the device is in landscape.
 *
 * @return true if the device is in landscape, otherwise false
 */
fun Context.isDeviceLandscape() = !isDevicePortrait()