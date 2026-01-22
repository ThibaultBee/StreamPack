package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Rect
import android.util.Size
import android.view.Surface

interface ISurfaceOutput {
    val targetSurface: Surface
    val targetResolution: Size
    val viewportRect: Rect
    val type: OutputType

    fun updateTransformMatrix(output: FloatArray, input: FloatArray)

    enum class OutputType {
        INTERNAL,
        BITMAP
    }
}