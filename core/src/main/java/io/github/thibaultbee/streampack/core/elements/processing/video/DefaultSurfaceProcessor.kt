/*
 * Copyright 2022 The Android Open Source Project
 * Copyright 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.annotation.IntRange
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.ISurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.preRotate
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.preVerticalFlip
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider.Companion.THREAD_NAME_GL
import io.github.thibaultbee.streampack.core.pipelines.IVideoDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


private class DefaultSurfaceProcessor(
    private val dynamicRangeProfile: DynamicRangeProfile,
    private val glThread: HandlerThreadExecutor,
) : ISurfaceProcessorInternal, SurfaceTexture.OnFrameAvailableListener {
    private val renderer = OpenGlRenderer()

    private val glHandler = glThread.handler

    private val isReleaseRequested = AtomicBoolean(false)
    private var isReleased = false

    private val textureMatrix = FloatArray(16)
    private val surfaceOutputMatrix = FloatArray(16)

    private val surfaceOutputs: MutableList<ISurfaceOutput> = mutableListOf()
    private val surfaceInputs: MutableList<SurfaceInput> = mutableListOf()
    private val surfaceInputsTimestampInNsMap: MutableMap<SurfaceTexture, Long> = hashMapOf()

    private val pendingSnapshots = mutableListOf<PendingSnapshot>()

    init {
        Logger.d(TAG, "Setting dynamic range profile to $dynamicRangeProfile")

        val future = submitSafely {
            renderer.init(dynamicRangeProfile)
        }
        try {
            future.get()
        } catch (e: Exception) {
            release()
            Logger.e(TAG, "Error while initializing renderer", e)
            throw e
        }
    }

    override fun createInputSurface(surfaceSize: Size, timestampOffsetInNs: Long): Surface {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("SurfaceProcessor is released")
        }

        val future = submitSafely {
            val surfaceTexture = SurfaceTexture(renderer.textureName)
            surfaceTexture.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
            surfaceTexture.setOnFrameAvailableListener(this, glHandler)
            if (dynamicRangeProfile.isHdr) {
                renderer.setInputFormat(GLUtils.InputFormat.YUV)
            }

            surfaceInputsTimestampInNsMap[surfaceTexture] = timestampOffsetInNs
            SurfaceInput(Surface(surfaceTexture), surfaceTexture)
        }

        val surfaceInput = future.get()
        surfaceInputs.add(surfaceInput)
        return surfaceInput.surface
    }

    override fun removeInputSurface(surface: Surface) {
        executeSafely {
            val surfaceInput = surfaceInputs.find { it.surface == surface }
            if (surfaceInput != null) {
                val surfaceTexture = surfaceInput.surfaceTexture
                surfaceTexture.setOnFrameAvailableListener(null, glHandler)
                surfaceTexture.release()
                surface.release()

                surfaceInputsTimestampInNsMap.remove(surfaceTexture)
                surfaceInputs.remove(surfaceInput)

                checkReadyToRelease()
            } else {
                Logger.w(TAG, "Surface not found")
            }
        }
    }

    override fun addOutputSurface(surfaceOutput: ISurfaceOutput) {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            if (!surfaceOutputs.contains(surfaceOutput)) {
                renderer.registerOutputSurface(surfaceOutput.descriptor.surface)
                surfaceOutputs.add(surfaceOutput)
            } else {
                Logger.w(TAG, "Surface already added")
            }
        }
    }

    private fun removeOutputSurfaceInternal(surfaceOutput: ISurfaceOutput) {
        if (surfaceOutputs.contains(surfaceOutput)) {
            renderer.unregisterOutputSurface(surfaceOutput.descriptor.surface)
            surfaceOutputs.remove(surfaceOutput)
        } else {
            Logger.w(TAG, "Surface not found")
        }
    }

    override fun removeOutputSurface(surfaceOutput: ISurfaceOutput) {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            removeOutputSurfaceInternal(surfaceOutput)
        }
    }

    override fun removeOutputSurface(surface: Surface) {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            val surfaceOutput =
                surfaceOutputs.firstOrNull { it.descriptor.surface == surface }
            if (surfaceOutput != null) {
                removeOutputSurfaceInternal(surfaceOutput)
            } else {
                Logger.w(TAG, "Surface not found")
            }
        }
    }

    private fun removeAllOutputSurfacesInternal() {
        surfaceOutputs.forEach { surfaceOutput ->
            renderer.unregisterOutputSurface(surfaceOutput.descriptor.surface)
        }
        surfaceOutputs.clear()
    }

    override fun removeAllOutputSurfaces() {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            removeAllOutputSurfacesInternal()
        }
    }

    override fun release() {
        if (isReleaseRequested.getAndSet(true)) {
            return
        }
        executeSafely(block = {
            if (!isReleased) {
                isReleased = true

                checkReadyToRelease()
            }
        })
    }

    private fun checkReadyToRelease() {
        if (isReleased && surfaceInputs.isEmpty()) {
            // Once release is called, we can stop sending frame to output surfaces.
            removeAllOutputSurfacesInternal()

            renderer.release()
            glThread.quit()
        }
    }

    override fun snapshot(
        @IntRange(from = 0, to = 359) rotationDegrees: Int
    ): ListenableFuture<Bitmap> {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("SurfaceProcessor is released")
        }
        return CallbackToFutureAdapter.getFuture { completer ->
            executeSafely {
                pendingSnapshots.add(PendingSnapshot(rotationDegrees, completer))
            }
        }
    }

    // Executed on GL thread
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (isReleaseRequested.get()) {
            return
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureMatrix)

        val timestamp =
            surfaceTexture.timestamp + (surfaceInputsTimestampInNsMap[surfaceTexture] ?: 0L)
        surfaceOutputs.filter { it.isStreaming() }.forEach {
            try {
                it.updateTransformMatrix(surfaceOutputMatrix, textureMatrix)
                renderer.render(
                    timestamp,
                    surfaceOutputMatrix,
                    it.descriptor.surface,
                    it.viewportRect
                )
            } catch (t: Throwable) {
                Logger.e(TAG, "Error while rendering frame", t)
            }
        }

        // Surface, size and transform matrix for JPEG Surface if exists
        if (pendingSnapshots.isNotEmpty()) {
            try {
                val first = surfaceOutputs.first()
                val snapshotOutput = Pair(
                    first.descriptor.resolution,
                    surfaceOutputMatrix.clone()
                )

                // Execute all pending snapshots.
                takeSnapshot(snapshotOutput)
            } catch (e: RuntimeException) {
                // Propagates error back to the app if failed to take snapshot.
                failAllPendingSnapshots(e)
            }
        }
    }

    /**
     * Takes a snapshot of the current frame and draws it to given JPEG surface.
     *
     * @param snapshotOutput The <Surface size, transform matrix> pair for drawing.
     */
    private fun takeSnapshot(snapshotOutput: Pair<Size, FloatArray>) {
        if (pendingSnapshots.isEmpty()) {
            // No pending snapshot requests, do nothing.
            return
        }

        // Write to JPEG surface, once for each snapshot request.
        try {
            for (pendingSnapshot in pendingSnapshots) {
                val (size, transform) = snapshotOutput

                // Take a snapshot of the current frame.
                val bitmap = getBitmap(size, transform, pendingSnapshot.rotationDegrees)

                // Complete the snapshot request.
                pendingSnapshot.completer.set(bitmap)
            }
            pendingSnapshots.clear()
        } catch (e: IOException) {
            failAllPendingSnapshots(e)
        }
    }

    private fun failAllPendingSnapshots(throwable: Throwable) {
        for (pendingSnapshot in pendingSnapshots) {
            pendingSnapshot.completer.setException(throwable)
        }
        pendingSnapshots.clear()
    }

    private fun getBitmap(
        size: Size,
        textureTransform: FloatArray,
        rotationDegrees: Int
    ): Bitmap {
        val snapshotTransform = textureTransform.clone()

        // Rotate the output if requested.
        snapshotTransform.preRotate(rotationDegrees.toFloat(), 0.5f, 0.5f)

        // Flip the snapshot. This is for reverting the GL transform added in SurfaceOutputImpl.
        snapshotTransform.preVerticalFlip(0.5f)

        // Update the size based on the rotation degrees.
        val rotatedSize = size.rotate(rotationDegrees)

        // Take a snapshot Bitmap and compress it to JPEG.
        return renderer.snapshot(rotatedSize, snapshotTransform)
    }

    private fun executeSafely(
        block: () -> Unit,
    ) {
        executeSafely(block, {}, {})
    }

    private fun <T> executeSafely(
        block: () -> T, onSuccess: ((T) -> Unit), onError: ((Throwable) -> Unit)
    ) {
        try {
            glHandler.post {
                if (isReleased) {
                    Logger.w(TAG, "SurfaceProcessor is released, block will not be executed")
                    onError(IllegalStateException("SurfaceProcessor is released"))
                } else {
                    try {
                        onSuccess(block())
                    } catch (t: Throwable) {
                        onError(t)
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Error while executing block", t)
            onError(t)
        }
    }

    private fun <T : Any> submitSafely(block: () -> T): ListenableFuture<T> {
        return CallbackToFutureAdapter.getFuture {
            executeSafely(block, { result -> it.set(result) }, { t -> it.setException(t) })
        }
    }

    companion object {
        private const val TAG = "SurfaceProcessor"
    }

    private data class SurfaceInput(val surface: Surface, val surfaceTexture: SurfaceTexture)

    private data class PendingSnapshot(
        @IntRange(from = 0, to = 359)
        val rotationDegrees: Int,
        val completer: CallbackToFutureAdapter.Completer<Bitmap>
    )
}

class DefaultSurfaceProcessorFactory :
    ISurfaceProcessorInternal.Factory {
    override fun create(
        dynamicRangeProfile: DynamicRangeProfile,
        dispatcherProvider: IVideoDispatcherProvider
    ): ISurfaceProcessorInternal {
        return DefaultSurfaceProcessor(
            dynamicRangeProfile,
            dispatcherProvider.createVideoHandlerExecutor(THREAD_NAME_GL)
        )
    }
}