package com.github.thibaultbee.streampack.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

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