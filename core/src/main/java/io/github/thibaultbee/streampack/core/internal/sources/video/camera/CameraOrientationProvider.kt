/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.sources.video.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.internal.orientation.AbstractSourceOrientationProvider
import io.github.thibaultbee.streampack.core.internal.utils.extensions.deviceOrientation
import io.github.thibaultbee.streampack.core.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.core.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.core.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.core.utils.cameraList
import io.github.thibaultbee.streampack.core.utils.getFacingDirection
import kotlin.math.max
import kotlin.math.min


class CameraOrientationProvider(private val context: Context, initialCameraId: String) :
    AbstractSourceOrientationProvider() {
    private val isFrontFacingMap =
        context.cameraList.associateWith { (context.getFacingDirection(it) == CameraCharacteristics.LENS_FACING_FRONT) }

    var cameraId: String = initialCameraId
        set(value) {
            if (field == value) {
                return
            }
            val orientationChanged = mirroredVertically != isFrontFacing(value)
            field = value
            if (orientationChanged) {
                listeners.forEach { it.onOrientationChanged() }
            }
        }

    override val orientation: Int
        get() = when (context.deviceOrientation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 90
            else -> 0
        }

    private fun isFrontFacing(cameraId: String): Boolean {
        return isFrontFacingMap[cameraId] ?: false
    }

    override val mirroredVertically: Boolean
        get() = isFrontFacing(cameraId)

    override fun getOrientedSize(size: Size): Size {
        return if (context.isDevicePortrait) {
            size.portraitize
        } else {
            size.landscapize
        }
    }

    override fun getDefaultBufferSize(size: Size): Size {
        return Size(max(size.width, size.height), min(size.width, size.height))
    }
}