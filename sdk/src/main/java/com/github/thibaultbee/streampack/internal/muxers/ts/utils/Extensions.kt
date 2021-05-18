package com.github.thibaultbee.streampack.internal.muxers.ts.utils

/**
 * Convert a Boolean to an Int.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toInt() = if (this) 1 else 0

