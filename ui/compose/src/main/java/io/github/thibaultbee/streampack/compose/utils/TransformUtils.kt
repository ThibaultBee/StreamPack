/*
 * Copyright 2026 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.compose.utils

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.layout.ContentScale
import io.github.thibaultbee.streampack.core.elements.utils.Position
import io.github.thibaultbee.streampack.core.elements.utils.ScaleMode

/**
 * Converts a Compose [ContentScale] to a StreamPack [ScaleMode].
 */
internal fun ContentScale.toScaleMode(): ScaleMode = when (this) {
    ContentScale.Crop -> ScaleMode.CROP
    ContentScale.Fit, ContentScale.Inside -> ScaleMode.FIT
    else -> throw IllegalArgumentException(
        "Unsupported ContentScale: $this. Only ContentScale.Crop, ContentScale.Fit, and ContentScale.Inside are supported."
    )
}

/**
 * Converts a Compose [Alignment] to a StreamPack 1D [Position].
 *
 * StreamPack's 1D position aligns excess content along whichever dimension differs from the container
 * aspect ratio (top/bottom for vertical excess, left/right for horizontal excess).
 */
internal fun Alignment.toPosition(): Position = when (this) {
    Alignment.Center -> Position.CENTER

    Alignment.TopCenter, Alignment.CenterStart, Alignment.TopStart, Alignment.TopEnd,
    AbsoluteAlignment.TopLeft, AbsoluteAlignment.TopRight, AbsoluteAlignment.CenterLeft -> Position.START

    Alignment.BottomCenter, Alignment.CenterEnd, Alignment.BottomStart, Alignment.BottomEnd,
    AbsoluteAlignment.BottomLeft, AbsoluteAlignment.BottomRight, AbsoluteAlignment.CenterRight -> Position.END

    is BiasAlignment -> {
        val bias = if (verticalBias != 0f) verticalBias else horizontalBias
        when {
            bias < 0f -> Position.START
            bias > 0f -> Position.END
            else -> Position.CENTER
        }
    }

    is BiasAbsoluteAlignment -> {
        val bias = if (verticalBias != 0f) verticalBias else horizontalBias
        when {
            bias < 0f -> Position.START
            bias > 0f -> Position.END
            else -> Position.CENTER
        }
    }

    else -> Position.CENTER
}
