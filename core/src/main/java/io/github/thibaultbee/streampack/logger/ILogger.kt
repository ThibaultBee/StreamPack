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
package io.github.thibaultbee.streampack.logger

import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer

/**
 * Logger interface.
 * You can implement a custom [ILogger] and pass it as a parameter of [BaseStreamer]
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