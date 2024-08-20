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
package io.github.thibaultbee.streampack.internal.utils.extensions

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.core.view.DisplayCompat
import io.github.thibaultbee.streampack.R
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.utils.OrientationUtils

/**
 * Returns the device orientation in degrees from the natural orientation: portrait.
 *
 * @return the device orientation in degrees
 */
val Context.deviceOrientationDegrees: Int
    get() {
        val displayManager = this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return OrientationUtils.getSurfaceOrientationDegrees(displayManager.getDisplay(Display.DEFAULT_DISPLAY).rotation)
    }

/**
 * Returns the device orientation in degrees from the natural orientation: portrait.
 *
 * @return the device orientation as [Surface.ROTATION_0], [Surface.ROTATION_90], [Surface.ROTATION_180] or [Surface.ROTATION_270]
 */
val Context.deviceOrientation: Int
    get() {
        val displayManager = this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return displayManager.getDisplay(Display.DEFAULT_DISPLAY).rotation
    }

/**
 * Check if the device is in portrait.
 *
 * @return true if the device is in portrait, otherwise false
 */
val Context.isDevicePortrait: Boolean
    get() {
        val display = ContextCompat.getDisplayOrDefault(this)
        val mode = DisplayCompat.getMode(this, display)
        val naturalResolution = Size(mode.physicalWidth, mode.physicalHeight)
        val naturalIsPortrait = naturalResolution.isPortrait
        return when (display.rotation) {
            Surface.ROTATION_0   -> naturalIsPortrait
            Surface.ROTATION_90  -> !naturalIsPortrait
            Surface.ROTATION_180 -> naturalIsPortrait
            Surface.ROTATION_270 -> !naturalIsPortrait
            else                 -> naturalIsPortrait
        }
    }


/**
 * Check if the device is in landscape.
 *
 * @return true if the device is in landscape, otherwise false
 */
val Context.isDeviceLandscape: Boolean
    get() = !isDevicePortrait

val Context.defaultTsServiceInfo
    get() = TsServiceInfo(
        TsServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        getString(R.string.ts_service_default_name),
        getString(R.string.ts_service_default_provider_name)
    )
