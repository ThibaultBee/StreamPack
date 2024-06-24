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
package io.github.thibaultbee.streampack.core.internal.utils.extensions

import android.util.Size
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Get the size in landscape orientation: the largest dimension as width and the smallest as height.
 *
 * @return the size in landscape orientation.
 */
val Size.landscapize: Size
    get() = Size(max(width, height), min(width, height))

/**
 * Get the size in portrait orientation: the smallest dimension as width and the largest as height.
 *
 * @return the size in portrait orientation.
 */
val Size.portraitize: Size
    get() = Size(min(width, height), max(width, height))

/**
 * Check if the size is in portrait orientation.
 */
val Size.isPortrait: Boolean
    get() = width < height

/**
 * Check if the size is in landscape orientation.
 */
val Size.isLandscape: Boolean
    get() = !isPortrait

/**
 * Find the closest size to the given size in a list of sizes.
 */
fun List<Size>.closestTo(size: Size): Size =
    this.minBy { abs((it.width * it.height) - (size.width * size.height)) }