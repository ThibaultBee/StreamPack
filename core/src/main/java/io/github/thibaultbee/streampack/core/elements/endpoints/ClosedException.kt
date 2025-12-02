package io.github.thibaultbee.streampack.core.elements.endpoints

import java.io.IOException

/**
 * Class that encapsulates closed errors
 */
class ClosedException(message: String? = null, cause: Throwable? = null) :
    IOException(message, cause) {
    constructor(message: String) : this(message, null)
    constructor(cause: Throwable) : this(cause.message, cause)
}