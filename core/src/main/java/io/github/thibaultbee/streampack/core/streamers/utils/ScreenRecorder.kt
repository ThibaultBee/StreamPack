package io.github.thibaultbee.streampack.core.streamers.utils

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

object ScreenRecorderUtils {
    /**
     * Creates a screen recorder intent.
     */
    fun createScreenRecorderIntent(context: Context): Intent =
        (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).run {
            createScreenCaptureIntent()
        }
}