package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.util.Size
import android.view.Surface

interface ISurfaceOutput {
    val surface: Surface
    val resolution: Size
    val type: OutputType

    fun updateTransformMatrix(output: FloatArray, input: FloatArray)

    enum class OutputType {
        INTERNAL,
        BITMAP
    }
}