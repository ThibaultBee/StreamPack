package io.github.thibaultbee.streampack.core.elements.utils.extensions

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}

internal fun <T> CancellableContinuation<T>.resumeWithExceptionIfActive(exception: Throwable) {
    if (isActive) {
        resumeWithException(exception)
    }
}