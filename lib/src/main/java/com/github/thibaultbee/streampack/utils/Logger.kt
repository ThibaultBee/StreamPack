package com.github.thibaultbee.streampack.utils

import android.util.Log

open class Logger {
    open fun e(tag: Any, message: String) = Log.e(tag.javaClass.simpleName, message)
    open fun w(tag: Any, message: String) = Log.w(tag.javaClass.simpleName, message)
    open fun i(tag: Any, message: String) = Log.i(tag.javaClass.simpleName, message)
    open fun v(tag: Any, message: String) = Log.v(tag.javaClass.simpleName, message)
    open fun d(tag: Any, message: String) = Log.d(tag.javaClass.simpleName, message)
}