package com.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.github.thibaultbee.streampack.app.R

object DialogUtils {
    fun showAlertDialog(context: Context, title: String, message: String = "") {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }

    fun showPermissionAlertDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.permission)
            .setMessage(R.string.permission_not_granted)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            .show()
    }
}