package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Color
import android.util.Size
import android.view.Surface
import androidx.annotation.ColorInt

interface ISurfaceOutput {
    val targetSurface: Surface
    val targetResolution: Size
    val type: OutputType

    @get:ColorInt
    val backgroundColor: Int
        get() = Color.BLACK

    fun updateTransformMatrix(output: FloatArray, input: FloatArray)

    enum class OutputType {
        INTERNAL,
        BITMAP
    }
}