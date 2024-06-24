package io.github.thibaultbee.streampack.core.logger

object Logger : ILogger {
    /**
     * The logger implementation.
     * Customize it by setting a new [ILogger] implementation.
     */
    var logger: ILogger = StreamPackLogger()

    override fun e(tag: String, message: String, tr: Throwable?) {
        logger.e(tag, message, tr)
    }

    override fun w(tag: String, message: String, tr: Throwable?) {
        logger.w(tag, message, tr)
    }

    override fun i(tag: String, message: String, tr: Throwable?) {
        logger.i(tag, message, tr)
    }

    override fun v(tag: String, message: String, tr: Throwable?) {
        logger.v(tag, message, tr)
    }

    override fun d(tag: String, message: String, tr: Throwable?) {
        logger.d(tag, message, tr)
    }
}