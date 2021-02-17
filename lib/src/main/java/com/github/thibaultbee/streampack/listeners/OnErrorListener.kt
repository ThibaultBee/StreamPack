package com.github.thibaultbee.streampack.listeners

import com.github.thibaultbee.streampack.utils.Error

interface OnErrorListener {
    fun onError(name: String, type: Error)
}