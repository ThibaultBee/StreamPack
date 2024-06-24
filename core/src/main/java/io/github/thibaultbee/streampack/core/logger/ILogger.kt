/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.logger

/**
 * Logger interface.
 * You can implement a custom [ILogger] and pass it to [Logger.logger]
 * implementation.
 */
interface ILogger {
    /**
     * Logs an error.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun e(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a warning.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun w(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs an info.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun i(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a verbose message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun v(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a debug message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun d(tag: String, message: String, tr: Throwable? = null)
}