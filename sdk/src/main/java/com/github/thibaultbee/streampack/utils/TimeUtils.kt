package com.github.thibaultbee.streampack.utils

object TimeUtils {
    /**
     * Gets current time from a unique source. Uses it for timestamp.
     */
    fun currentTime() = System.nanoTime() / 1000 // to µs
}