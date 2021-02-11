package com.github.thibaultbee.srtstreamer.listeners

import com.github.thibaultbee.srtstreamer.utils.Error

interface OnErrorListener {
    fun onError(name: String, type: Error)
}