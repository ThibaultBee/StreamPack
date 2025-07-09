/*
 * Copyright (C) 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Rect
import android.opengl.Matrix
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor


class IdentitySurfaceOutput(
    override val descriptor: SurfaceDescriptor,
    override val isStreaming: () -> Boolean,
    override val viewportRect: Rect = Rect(
        0,
        0,
        descriptor.resolution.width,
        descriptor.resolution.height
    )
) : ISurfaceOutput {
    override fun updateTransformMatrix(output: FloatArray, input: FloatArray) {
        Matrix.multiplyMM(
            output, 0, input, 0, GLUtils.IDENTITY_MATRIX, 0
        )
    }
}

