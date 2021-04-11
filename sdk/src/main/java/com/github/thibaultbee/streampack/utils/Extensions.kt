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

import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

/**
 * Check if mime type is a video mime type
 * @return true if mime type is video, otherwise false
 */
fun String.isVideo() = this.startsWith("video")

/**
 * Check if mime type is an audio mime type
 * @return true if mime type is audio, otherwise false
 */
fun String.isAudio() = this.startsWith("audio")

/**
 * Convert a Boolean to an Int
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toInt() = if (this) 1 else 0

/**
 * Returns ByteBuffer array even if [ByteBuffer.hasArray] returnd false
 * @return [ByteArray] extracted from [ByteBuffer]
 */
fun ByteBuffer.extractArray(): ByteArray {
    return if (this.hasArray()) {
        this.array()
    } else {
        val byteArray = ByteArray(this.remaining())
        this.get(byteArray)
        byteArray
    }
}


/**
 * Returns Camera orientation
 * @return an integer equals to the current camera orientation
 */
fun Context.getCameraOrientation(): Int {
    val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return when (val displayRotation = windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_0 -> 90
        Surface.ROTATION_90 -> 0
        Surface.ROTATION_180 -> 270
        Surface.ROTATION_270 -> 180
        else -> throw UnsupportedOperationException(
            "Unsupported display rotation: $displayRotation"
        )
    }
}

/**
 * Check if camera is in portrait
 * @return true if camera is in portrait, otherwise false
 */
fun Context.isCameraPortrait(): Boolean {
    val orientation = this.getCameraOrientation()
    return orientation == 90 || orientation == 270
}

/**
 * Check if permission is granted
 * @return true if permission has been granted, otherwise false
 */
fun Context.hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    this, permission
) == PackageManager.PERMISSION_GRANTED

/**
 * Check if multiple permissions are granted
 * @return true if all permissions have been granted, otherwise false
 */
fun Context.hasPermissions(permissions: List<String>) = permissions.none {
    ContextCompat.checkSelfPermission(
        this, it
    ) != PackageManager.PERMISSION_GRANTED
}
