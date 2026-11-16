/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import android.media.MediaCodec
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel

/**
 * Where an encoder receives its [MediaCodec.Callback] and processes it.
 *
 * From API 23, callbacks are delivered on a handler thread and processed there. Before, they are
 * delivered on the main looper and processed on a dispatcher.
 */
internal sealed class MediaCodecCallbackCompat {
    abstract val processDispatcher: CoroutineDispatcher

    abstract fun setCallback(mediaCodec: MediaCodec, callback: MediaCodec.Callback)

    abstract fun release()

    @RequiresApi(Build.VERSION_CODES.M)
    private class HandlerCallback(handlerExecutorBuilder: () -> HandlerThreadExecutor) :
        MediaCodecCallbackCompat() {
        private val handlerExecutor = lazy(handlerExecutorBuilder)

        override val processDispatcher by lazy {
            handlerExecutor.value.handler.asCoroutineDispatcher().immediate
        }

        override fun setCallback(mediaCodec: MediaCodec, callback: MediaCodec.Callback) {
            mediaCodec.setCallback(callback, handlerExecutor.value.handler)
        }

        override fun release() {
            if (handlerExecutor.isInitialized()) {
                handlerExecutor.value.quit()
            }
        }
    }

    private class DispatcherCallback(override val processDispatcher: CoroutineDispatcher) :
        MediaCodecCallbackCompat() {
        override fun setCallback(mediaCodec: MediaCodec, callback: MediaCodec.Callback) {
            mediaCodec.setCallback(callback)
        }

        override fun release() {
            processDispatcher.cancel()
        }
    }

    companion object {
        fun create(
            handlerExecutorBuilder: () -> HandlerThreadExecutor,
            dispatcherBuilder: () -> CoroutineDispatcher
        ): MediaCodecCallbackCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HandlerCallback(handlerExecutorBuilder)
        } else {
            DispatcherCallback(dispatcherBuilder())
        }
    }
}
