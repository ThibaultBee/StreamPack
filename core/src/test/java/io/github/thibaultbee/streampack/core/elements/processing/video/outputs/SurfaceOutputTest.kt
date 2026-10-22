/*
 * Copyright 2026 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.PointF
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.utils.Position
import io.github.thibaultbee.streampack.core.elements.utils.ScaleMode
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurfaceOutputTest {

    private fun transformPoint(
        output: SurfaceOutput,
        point: PointF
    ): PointF {
        val inputMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
        val outputMatrix = FloatArray(16)
        output.updateTransformMatrix(outputMatrix, inputMatrix)

        val p = floatArrayOf(point.x, point.y, 0f, 1f)
        val res = FloatArray(4)
        Matrix.multiplyMV(res, 0, outputMatrix, 0, p, 0)
        return PointF(res[0], res[1])
    }

    private fun createSurfaceOutput(
        sourceResolution: Size,
        targetResolution: Size,
        rotationDegrees: Int = 0,
        isMirror: Boolean = false,
        needMirroring: Boolean = false,
        scaleMode: ScaleMode = ScaleMode.CROP,
        position: Position = Position.CENTER
    ): Pair<SurfaceOutput, () -> Unit> {
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val infoProvider = object : DefaultSourceInfoProvider(
            isMirror = isMirror,
            rotationDegrees = rotationDegrees
        ) {
            override fun getRelativeRotationDegrees(
                targetRotation: Int,
                requiredMirroring: Boolean
            ): Int = rotationDegrees
        }

        val output = SurfaceOutput(
            targetSurface = surface,
            targetResolution = targetResolution,
            targetRotation = Surface.ROTATION_0,
            isStreaming = { true },
            sourceResolution = sourceResolution,
            needMirroring = needMirroring,
            sourceInfoProvider = infoProvider,
            scaleMode = scaleMode,
            position = position
        )

        val cleanup = {
            surface.release()
            surfaceTexture.release()
        }
        return Pair(output, cleanup)
    }

    @Test
    fun `same resolution maintains exact coordinates`() {
        val (output, cleanup) = createSurfaceOutput(
            sourceResolution = Size(1920, 1080),
            targetResolution = Size(1920, 1080)
        )
        try {
            val p0 = transformPoint(output, PointF(0f, 0f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)

            val p1 = transformPoint(output, PointF(1f, 1f))
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
        } finally {
            cleanup()
        }
    }

    @Test
    fun `crop scaleMode at 0deg`() {
        // Source 1920x1080 (16:9), Target 1080x1080 (1:1)
        val src = Size(1920, 1080)
        val dst = Size(1080, 1080)

        // Center
        val (outCenter, cleanCenter) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.CROP, position = Position.CENTER)
        try {
            val p0 = transformPoint(outCenter, PointF(0f, 0f))
            val p1 = transformPoint(outCenter, PointF(1f, 1f))
            val pMid = transformPoint(outCenter, PointF(0.5f, 0.5f))
            assertEquals(0.21875f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)
            assertEquals(0.78125f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
            assertEquals(0.5f, pMid.x, 0.001f)
            assertEquals(0.5f, pMid.y, 0.001f)
        } finally {
            cleanCenter()
        }

        // Start
        val (outStart, cleanStart) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.CROP, position = Position.START)
        try {
            val p0 = transformPoint(outStart, PointF(0f, 0f))
            val p1 = transformPoint(outStart, PointF(1f, 1f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)
            assertEquals(0.5625f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
        } finally {
            cleanStart()
        }

        // End
        val (outEnd, cleanEnd) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.CROP, position = Position.END)
        try {
            val p0 = transformPoint(outEnd, PointF(0f, 0f))
            val p1 = transformPoint(outEnd, PointF(1f, 1f))
            assertEquals(0.4375f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
        } finally {
            cleanEnd()
        }
    }

    @Test
    fun `fit scaleMode at 0deg`() {
        // Source 1920x1080 (16:9), Target 1080x1080 (1:1)
        val src = Size(1920, 1080)
        val dst = Size(1080, 1080)

        // Center: vertical letterbox
        val (outCenter, cleanCenter) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.FIT, position = Position.CENTER)
        try {
            val p0 = transformPoint(outCenter, PointF(0f, 0f))
            val p1 = transformPoint(outCenter, PointF(1f, 1f))
            val pMid = transformPoint(outCenter, PointF(0.5f, 0.5f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(-0.38888f, p0.y, 0.001f)
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(1.38888f, p1.y, 0.001f)
            assertEquals(0.5f, pMid.x, 0.001f)
            assertEquals(0.5f, pMid.y, 0.001f)
        } finally {
            cleanCenter()
        }

        // Start: video aligned to start (top)
        val (outStart, cleanStart) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.FIT, position = Position.START)
        try {
            val p0 = transformPoint(outStart, PointF(0f, 0f))
            val p1 = transformPoint(outStart, PointF(1f, 1f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(-0.77777f, p0.y, 0.001f)
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
        } finally {
            cleanStart()
        }

        // End: video aligned to end (bottom)
        val (outEnd, cleanEnd) = createSurfaceOutput(src, dst, scaleMode = ScaleMode.FIT, position = Position.END)
        try {
            val p0 = transformPoint(outEnd, PointF(0f, 0f))
            val p1 = transformPoint(outEnd, PointF(1f, 1f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(1.77777f, p1.y, 0.001f)
        } finally {
            cleanEnd()
        }
    }

    @Test
    fun `crop and fit scaleMode at 90deg`() {
        val src = Size(1920, 1080)
        val dst = Size(1080, 1080)

        val (outCrop, cleanCrop) = createSurfaceOutput(src, dst, rotationDegrees = 90, scaleMode = ScaleMode.CROP, position = Position.CENTER)
        try {
            val p0 = transformPoint(outCrop, PointF(0f, 0f))
            val p1 = transformPoint(outCrop, PointF(1f, 1f))
            assertEquals(0f, p0.x, 0.001f)
            assertEquals(0.21875f, p0.y, 0.001f)
            assertEquals(1f, p1.x, 0.001f)
            assertEquals(0.78125f, p1.y, 0.001f)
        } finally {
            cleanCrop()
        }

        val (outFit, cleanFit) = createSurfaceOutput(src, dst, rotationDegrees = 90, scaleMode = ScaleMode.FIT, position = Position.CENTER)
        try {
            val p0 = transformPoint(outFit, PointF(0f, 0f))
            val p1 = transformPoint(outFit, PointF(1f, 1f))
            assertEquals(-0.38888f, p0.x, 0.001f)
            assertEquals(0f, p0.y, 0.001f)
            assertEquals(1.38888f, p1.x, 0.001f)
            assertEquals(1f, p1.y, 0.001f)
        } finally {
            cleanFit()
        }
    }

    @Test
    fun `surfaceDescriptor factory propagates scaleMode and position`() {
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        try {
            val descriptor = SurfaceDescriptor(
                surface = surface,
                resolution = Size(1080, 1080),
                targetRotation = Surface.ROTATION_0,
                scaleMode = ScaleMode.FIT,
                position = Position.START
            )

            val output = SurfaceOutput(
                descriptor = descriptor,
                isStreaming = { true },
                sourceResolution = Size(1920, 1080),
                needMirroring = false,
                sourceInfoProvider = DefaultSourceInfoProvider()
            )

            assertEquals(ScaleMode.FIT, output.scaleMode)
            assertEquals(Position.START, output.position)
        } finally {
            surface.release()
            surfaceTexture.release()
        }
    }
}
