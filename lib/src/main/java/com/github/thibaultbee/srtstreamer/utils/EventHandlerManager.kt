package com.github.thibaultbee.srtstreamer.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.github.thibaultbee.srtstreamer.interfaces.BaseInterface
import com.github.thibaultbee.srtstreamer.interfaces.OnConnectionListener
import com.github.thibaultbee.srtstreamer.interfaces.OnErrorListener

open class EventHandlerManager: BaseInterface {
    protected val eventHandler by lazy {
        EventHandler(this)
    }
    open var onErrorListener: OnErrorListener? = null
    var onConnectionListener: OnConnectionListener? = null

    fun reportError(error: Error) {
        val msg = eventHandler.obtainMessage(BASE_ERROR, error.ordinal, 0)
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
        private val base: BaseInterface,
        looper: Looper = Looper.myLooper() ?: Looper.getMainLooper()
    ) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BASE_ERROR -> onErrorListener?.onError(
                    base,
                    Error.valueOf(msg.arg1) ?: Error.UNKNOWN
                )
                BASE_CONNECTION_LOST -> onConnectionListener?.onLost()
            }
        }
    }
}