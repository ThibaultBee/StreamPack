package com.github.thibaultbee.streampack.utils

class FakeLogger : Logger() {
    override fun e(tag: Any, message: String) = println("E:$message")
    override fun w(tag: Any, message: String) = println("W:$message")
    override fun i(tag: Any, message: String) = println("I:$message")
    override fun v(tag: Any, message: String) = println("V:$message")
    override fun d(tag: Any, message: String) = println("D:$message")
}