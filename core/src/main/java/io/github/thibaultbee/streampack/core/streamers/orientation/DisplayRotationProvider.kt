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
import android.hardware.display.DisplayManager
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation

/**
 * A [RotationProvider] that provides display rotation.
 * It uses [DisplayManager] to get display rotation.
 * It follows the orientation of the display only. If device or application are locked in a specific
 * orientation, it will not change.
 *
 * It will notify listeners when the display orientation changes.
 *
 * @param context The application context
 */
class DisplayRotationProvider(val context: Context) : RotationProvider() {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var _rotation = context.displayRotation

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            val newRotation = context.displayRotation

            if (_rotation != newRotation) {
                _rotation = newRotation
                notifyListeners(newRotation)
            }
        }
    }

    override val rotation: Int
        get() = _rotation

    override fun onFirstListenerAdded() {
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun onLastListenerRemoved() {
        displayManager.unregisterDisplayListener(displayListener)
    }
}