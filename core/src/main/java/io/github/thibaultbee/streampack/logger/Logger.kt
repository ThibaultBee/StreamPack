package io.github.thibaultbee.streampack.logger

object Logger : ILogger {
    /**
     * The logger implementation.
     * Customize it by setting a new [ILogger] implementation.
     */
    var logger: ILogger = StreamPackLogger()

    override fun e(obj: Any, message: String, tr: Throwable?) {
        logger.e(obj, message, tr)
    }

    override fun w(obj: Any, message: String, tr: Throwable?) {
        logger.w(obj, message, tr)
    }

    override fun i(obj: Any, message: String, tr: Throwable?) {
        logger.i(obj, message, tr)
    }

    override fun v(obj: Any, message: String, tr: Throwable?) {
        logger.v(obj, message, tr)
    }

    override fun d(obj: Any, message: String, tr: Throwable?) {
        logger.d(obj, message, tr)
    }
}