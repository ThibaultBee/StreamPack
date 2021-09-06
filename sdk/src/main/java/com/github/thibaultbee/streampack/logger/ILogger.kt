package com.github.thibaultbee.streampack.logger

import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer

/**
 * Logger interface.
 * You can implement a custom [ILogger] and pass it as a parameter of [BaseCameraStreamer]
 * implementation.
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