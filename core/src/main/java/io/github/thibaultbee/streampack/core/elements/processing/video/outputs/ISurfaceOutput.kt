package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Rect
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor

interface ISurfaceOutput {
    val descriptor: SurfaceDescriptor
    val isStreaming: () -> Boolean
    val viewportRect: Rect

    fun updateTransformMatrix(output: FloatArray, input: FloatArray)
}