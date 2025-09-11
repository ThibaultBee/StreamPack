package io.github.thibaultbee.streampack.core.elements.utils.extensions

import android.util.Range

/**
 * Converts a [Range]<Int> to a [Range]<Float>.
 */
fun Range<Int>.toFloatRange(): Range<Float> {
    return Range(this.lower.toFloat(), this.upper.toFloat())
}

/**
 * Converts a list of [Range]<Int> to a list of [Range]<Float>.
 */
fun List<Range<Int>>.toFloatRanges(): List<Range<Float>> {
    return this.map { it.toFloatRange() }
}