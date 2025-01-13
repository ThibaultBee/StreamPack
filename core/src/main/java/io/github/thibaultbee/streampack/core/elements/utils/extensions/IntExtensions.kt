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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils

/**
 * Returns the rotation in degrees from [Int] rotation.
 */
internal val Int.rotationToDegrees: Int
    @IntRange(from = 0, to = 359)
    get() = OrientationUtils.getSurfaceRotationDegrees(this)

/**
 * Whether the integer is a multiple of 90.
 */
internal val Int.is90Multiple: Boolean
    get() = this % 90 == 0

/**
 * Clamps the integer to the nearest multiple of 90.
 */
internal val Int.clamp90: Int
    get() = (this + 45) / 90 * 90

/**
 * Converts the integer to a value within 360 degrees.
 */
internal val Int.within360: Int
    get() {
        return (this % 360 + 360) % 360
    }

