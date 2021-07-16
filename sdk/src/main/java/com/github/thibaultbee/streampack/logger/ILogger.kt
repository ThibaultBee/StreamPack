package com.github.thibaultbee.streampack.logger

/**
 * Classic logger interface
 */
interface ILogger {
    /**
     * Logs an error.
     *
     * @param obj calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun e(obj: Any, message: String, tr: Throwable? = null)

    /**
     * Logs a warning.
     *
     * @param obj calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun w(obj: Any, message: String, tr: Throwable? = null)

    /**
     * Logs an info.
     *
     * @param obj calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun i(obj: Any, message: String, tr: Throwable? = null)

    /**
     * Logs a verbose message.
     *
     * @param obj calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun v(obj: Any, message: String, tr: Throwable? = null)

    /**
     * Logs a debug message.
     *
     * @param obj calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun d(obj: Any, message: String, tr: Throwable? = null)
}