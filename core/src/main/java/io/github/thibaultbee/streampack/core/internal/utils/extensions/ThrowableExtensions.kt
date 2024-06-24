package io.github.thibaultbee.streampack.core.internal.utils.extensions

/**
 * Get root cause. This code is from guava Throwables API.
 */
val Throwable.rootCause: Throwable
    get() {
        // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
        // the slower pointer, then there's a loop.
        var throwable = this
        var slowPointer: Throwable? = throwable
        var advanceSlowPointer = false

        while (throwable.cause != null) {
            throwable = throwable.cause!!
            if (throwable === slowPointer) {
                throw IllegalArgumentException("Loop in causal chain detected.", throwable)
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer!!.cause
            }
            advanceSlowPointer = !advanceSlowPointer // only advance every other iteration
        }
        return throwable
    }