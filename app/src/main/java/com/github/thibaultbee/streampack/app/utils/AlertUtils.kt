package com.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class AlertUtils {
    companion object {
        fun show(context: Context, title: String, message: String = "") {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                .show()
        }

    }
}