package com.github.thibaultbee.srtstreamer.app.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.github.thibaultbee.srtstreamer.app.R

class AlertUtils {
    companion object {
        fun show(context: Context, title: String, message: String = "") {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dismiss) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
                .show()
        }

    }
}