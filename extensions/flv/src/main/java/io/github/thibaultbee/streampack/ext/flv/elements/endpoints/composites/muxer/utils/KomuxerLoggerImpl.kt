/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils

import io.github.komedia.komuxer.logger.IKomuxerLogger
import io.github.thibaultbee.streampack.core.logger.Logger

internal class KomuxerLoggerImpl : IKomuxerLogger {
    override fun e(tag: String, message: String, tr: Throwable?) = Logger.e(tag, message, tr)

    override fun w(tag: String, message: String, tr: Throwable?) = Logger.w(tag, message, tr)

    override fun i(tag: String, message: String, tr: Throwable?) = Logger.i(tag, message, tr)

    override fun v(tag: String, message: String, tr: Throwable?) = Logger.v(tag, message, tr)

    override fun d(tag: String, message: String, tr: Throwable?) = Logger.d(tag, message, tr)
}