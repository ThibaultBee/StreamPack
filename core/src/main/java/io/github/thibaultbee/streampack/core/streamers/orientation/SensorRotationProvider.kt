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
package io.github.thibaultbee.streampack.core.streamers.orientation

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import io.github.thibaultbee.streampack.core.internal.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.utils.extensions.clamp90


/**
 * A [RotationProvider] that provides device rotation.
 * It uses [OrientationEventListener] to get device orientation.
 * It follows the orientation of the sensor, so it will change when the device is rotated.
 *
 * It will notify listeners when the device orientation changes.
 *
 * @param context The application context
 */
class SensorRotationProvider(val context: Context) : RotationProvider() {
    private val lock = Any()
    private var _rotation = context.displayRotation

    private val eventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val newRotation = orientationToSurfaceRotation(orientation.clamp90)

                if (_rotation != newRotation) {
                    _rotation = newRotation

                    synchronized(lock) {
                        listeners.forEach { it.onOrientationChanged(newRotation) }
                    }
                }
            }
        }
    }

    override val rotation: Int
        get() = _rotation

    override fun addListener(listener: IRotationProvider.Listener) {
        synchronized(lock) {
            super.addListener(listener)
            eventListener.enable()
        }
    }

    override fun removeListener(listener: IRotationProvider.Listener) {
        synchronized(lock) {
            super.removeListener(listener)
            if (listeners.isEmpty()) {
                eventListener.disable()
            }
        }
    }

    companion object {
        /**
         * Converts orientation degrees to [Surface] rotation.
         */
        private fun orientationToSurfaceRotation(rotationDegrees: Int): Int {
            return if (rotationDegrees >= 315 || rotationDegrees < 45) {
                Surface.ROTATION_0
            } else if (rotationDegrees >= 225) {
                Surface.ROTATION_90
            } else if (rotationDegrees >= 135) {
                Surface.ROTATION_180
            } else {
                Surface.ROTATION_270
            }
        }

    }
}