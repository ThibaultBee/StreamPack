package com.github.thibaultbee.srtstreamer.interfaces

import com.github.thibaultbee.srtstreamer.utils.Error

interface OnErrorListener {
    fun onError(base: BaseInterface, type: Error)
}