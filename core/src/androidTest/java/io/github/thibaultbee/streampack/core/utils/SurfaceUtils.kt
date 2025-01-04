package io.github.thibaultbee.streampack.core.utils

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface

object SurfaceUtils {
    fun createSurface(resolution: Size): Surface {
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
        return Surface(surfaceTexture)
    }
}