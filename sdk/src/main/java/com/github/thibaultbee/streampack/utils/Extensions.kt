package com.github.thibaultbee.streampack.utils

import android.content.Context
import android.content.pm.PackageManager
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
