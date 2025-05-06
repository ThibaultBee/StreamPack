package io.github.thibaultbee.streampack.core.pipelines.utils


fun MultiException(vararg throwables: Throwable) = MultiException(throwables.toList())

/**
 * Exception that wraps multiple exceptions.
 */
class MultiException(val throwables: Iterable<Throwable>) :
    Exception("Multiple exceptions: ${throwables.joinToString("\n") { it.message ?: "" }}")