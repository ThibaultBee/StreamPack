/*
 * Copyright (C) 2022 Thibault B.
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

import android.graphics.PointF
import android.graphics.Rect
import android.util.Range
import android.util.Rational

internal fun Any.numOfBits(): Int {
    return when (this) {
        is Byte -> Byte.SIZE_BITS
        is Short -> Short.SIZE_BITS
        is Int -> Int.SIZE_BITS
        is Long -> Long.SIZE_BITS
        is Float -> Float.SIZE_BITS
        is Double -> Double.SIZE_BITS
        is Boolean -> 1
        is Char -> Char.SIZE_BITS
        is String -> Char.SIZE_BITS * length
        is ByteArray -> Byte.SIZE_BITS * size
        is ShortArray -> Short.SIZE_BITS * size
        is IntArray -> Int.SIZE_BITS * size
        is LongArray -> Long.SIZE_BITS * size
        is FloatArray -> Float.SIZE_BITS * size
        is DoubleArray -> Double.SIZE_BITS * size
        is BooleanArray -> size
        is CharArray -> Char.SIZE_BITS * size
        else -> throw IllegalArgumentException("Unsupported type: ${this.javaClass.name}")
    }
}

internal fun <T : Comparable<T>> T.clamp(min: T, max: T): T {
    return if (max >= min) {
        if (this < min) min else if (this > max) max else this
    } else {
        if (this < max) max else if (this > min) min else this
    }
}

internal fun <T : Comparable<T>> T.clamp(range: Range<T>) =
    this.clamp(range.lower, range.upper)

internal val PointF.isNormalized: Boolean
    get() = x in 0f..1f && y in 0f..1f

internal fun PointF.rotate(rotation: Int): PointF {
    return when (rotation) {
        0 -> this
        90 -> PointF(y, 1 - x)
        180 -> PointF(1 - x, 1 - y)
        270 -> PointF(1 - y, x)
        else -> throw IllegalArgumentException("Unsupported rotation: $rotation")
    }
}

internal fun PointF.normalize(width: Int, height: Int): PointF {
    return PointF(x / width, y / height)
}

internal fun PointF.normalize(rect: Rect): PointF {
    return PointF(x / rect.width(), y / rect.height())
}

internal fun Rational.flip() = Rational(denominator, numerator)
