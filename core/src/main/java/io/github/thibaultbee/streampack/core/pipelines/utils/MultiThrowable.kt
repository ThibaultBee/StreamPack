package io.github.thibaultbee.streampack.core.pipelines.utils


fun MultiException(vararg throwables: Throwable) = MultiThrowable(throwables.toList())

/**
 * Exception that wraps multiple exceptions.
 */
class MultiThrowable(val throwables: Iterable<Throwable>) :
    Exception("Multiple exceptions: ${throwables.joinToString("\n") { it.message ?: "" }}")