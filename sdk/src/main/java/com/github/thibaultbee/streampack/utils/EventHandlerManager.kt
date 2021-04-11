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
package com.github.thibaultbee.streampack.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener

open class EventHandlerManager {
    private val eventHandler by lazy {
        EventHandler(this.javaClass.simpleName)
    }
    open var onErrorListener: OnErrorListener? = null
    var onConnectionListener: OnConnectionListener? = null

    fun reportError(error: Error) {
        val msg = eventHandler.obtainMessage(BASE_ERROR, error.ordinal, 0)
        eventHandler.sendMessage(msg)
    }


    fun reportError(error: Exception) {
        val msg = eventHandler.obtainMessage(BASE_ERROR, error.message)
        eventHandler.sendMessage(msg)
    }

    fun reportConnectionLost() {
        val msg = eventHandler.obtainMessage(BASE_CONNECTION_LOST)
        eventHandler.sendMessage(msg)
    }

    companion object {
        private const val BASE_ERROR = 0
        private const val BASE_CONNECTION_LOST = 100
    }

    open inner class EventHandler(
        private val name: String,
        looper: Looper = Looper.myLooper() ?: Looper.getMainLooper()
    ) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BASE_ERROR -> onErrorListener?.onError(
                    name,
                    Error.valueOf(msg.arg1) ?: Error.UNKNOWN
                )
                BASE_CONNECTION_LOST -> onConnectionListener?.onLost()
            }
        }
    }
}