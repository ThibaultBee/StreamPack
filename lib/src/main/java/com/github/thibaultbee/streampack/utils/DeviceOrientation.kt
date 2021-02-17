package com.github.thibaultbee.streampack.utils

import android.content.Context
import android.view.Surface
import android.view.WindowManager


class DeviceOrientation {
    companion object {
        fun get(context: Context): Int {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> 90
                Surface.ROTATION_90 -> 0
                Surface.ROTATION_180 -> 270
                Surface.ROTATION_270 -> 180
                else -> 0
            }
        }
    }
}