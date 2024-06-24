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

import android.util.Log

/**
 * Implementation of [ILogger]. Use as default logger in StreamPack.
 * It calls Android [Log].
 */
class StreamPackLogger : ILogger {
    override fun e(tag: String, message: String, tr: Throwable?) {
        Log.e(tag, message, tr)
    }

    override fun w(tag: String, message: String, tr: Throwable?) {
        Log.w(tag, message, tr)
    }

    override fun i(tag: String, message: String, tr: Throwable?) {
        Log.i(tag, message, tr)
    }

    override fun v(tag: String, message: String, tr: Throwable?) {
        Log.v(tag, message, tr)
    }

    override fun d(tag: String, message: String, tr: Throwable?) {
        Log.d(tag, message, tr)
    }
}
