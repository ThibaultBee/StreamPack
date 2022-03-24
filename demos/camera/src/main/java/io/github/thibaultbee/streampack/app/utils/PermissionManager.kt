package io.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionManager {
    companion object {
        fun hasPermissions(context: Context, vararg permissions: String): Boolean =
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
    }
}