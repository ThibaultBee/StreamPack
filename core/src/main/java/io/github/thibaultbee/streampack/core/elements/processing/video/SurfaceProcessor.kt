package io.github.thibaultbee.streampack.core.elements.processing.video

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.concurrent.futures.CallbackToFutureAdapter
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AbstractSurfaceOutput
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean


class SurfaceProcessor(
    val dynamicRangeProfile: DynamicRangeProfile
) : ISurfaceProcessorInternal, SurfaceTexture.OnFrameAvailableListener {
    private val renderer = OpenGlRenderer()

    private val isReleaseRequested = AtomicBoolean(false)
    private var isReleased = false

    private val textureMatrix = FloatArray(16)
    private val surfaceOutputMatrix = FloatArray(16)

    private val surfaceOutputs: MutableList<AbstractSurfaceOutput> = mutableListOf()
    private val surfaceInputs: MutableList<SurfaceInput> = mutableListOf()

    private val glThread = HandlerThread("GL Thread").apply {
        start()
    }
    private val glHandler = Handler(glThread.looper)

    init {
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

    override fun createInputSurface(surfaceSize: Size): Surface? {
        if (isReleaseRequested.get()) {
            return null
        }

        val future = submitSafely {
            val surfaceTexture = SurfaceTexture(renderer.textureName)
            surfaceTexture.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
            surfaceTexture.setOnFrameAvailableListener(this, glHandler)
            SurfaceInput(Surface(surfaceTexture), surfaceTexture)
        }

        val surfaceInput = future.get()
        surfaceInputs.add(surfaceInput)
        return surfaceInput.surface
    }

    override fun updateInputSurface(surface: Surface, surfaceSize: Size) {
        executeSafely {
            val surfaceInput = surfaceInputs.find { it.surface == surface }
            if (surfaceInput != null) {
                surfaceInput.surfaceTexture.setDefaultBufferSize(
                    surfaceSize.width,
                    surfaceSize.height
                )
            } else {
                Logger.w(TAG, "Surface not found")
            }
        }
    }

    override fun removeInputSurface(surface: Surface) {
        executeSafely {
            val surfaceInput = surfaceInputs.find { it.surface == surface }
            if (surfaceInput != null) {
                val surfaceTexture = surfaceInput.surfaceTexture
                surfaceTexture.setOnFrameAvailableListener(null, glHandler)
                surfaceTexture.release()
                surface.release()

                surfaceInputs.remove(surfaceInput)

                checkReadyToRelease()
            } else {
                Logger.w(TAG, "Surface not found")
            }
        }
    }

    override fun addOutputSurface(surfaceOutput: AbstractSurfaceOutput) {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            if (!surfaceOutputs.contains(surfaceOutput)) {
                renderer.registerOutputSurface(surfaceOutput.surface)
                surfaceOutputs.add(surfaceOutput)
            } else {
                Logger.w(TAG, "Surface already added")
            }
        }
    }

    override fun removeOutputSurface(surfaceOutput: AbstractSurfaceOutput) {
        if (isReleaseRequested.get()) {
            return
        }

        executeSafely {
            if (surfaceOutputs.contains(surfaceOutput)) {
                renderer.unregisterOutputSurface(surfaceOutput.surface)
                surfaceOutputs.remove(surfaceOutput)
            } else {
                Logger.w(TAG, "Surface not found")
            }
        }
    }

    override fun removeOutputSurface(surface: Surface) {
        val surfaceOutput = surfaceOutputs.firstOrNull { it.surface == surface }
        if (surfaceOutput != null) {
            removeOutputSurface(surfaceOutput)
        } else {
            Logger.w(TAG, "Surface not found")
        }
    }

    override fun removeAllOutputSurfaces() {
        surfaceOutputs.forEach {
            try {
                removeOutputSurface(it)
            } catch (e: Exception) {
                Logger.w(TAG, "Error while removing output surface", e)
            }
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
            surfaceOutputs.forEach { it.close() }
            removeAllOutputSurfaces()

            renderer.release()
            glThread.quit()
        }
    }


    private fun onFrameAvailableInternal(surfaceTexture: SurfaceTexture) {
        if (isReleaseRequested.get()) {
            return
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureMatrix)

        surfaceOutputs.forEach {
            try {
                it.updateTransformMatrix(surfaceOutputMatrix, textureMatrix)
                renderer.render(surfaceTexture.timestamp, surfaceOutputMatrix, it.surface)
            } catch (e: Exception) {
                Logger.e(TAG, "Error while rendering frame", e)
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        onFrameAvailableInternal(surfaceTexture)
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

    private fun <T> submitSafely(block: () -> T): Future<T> {
        return CallbackToFutureAdapter.getFuture {
            executeSafely(block, { result -> it.set(result) }, { t -> it.setException(t) })
        }
    }

    companion object {
        private const val TAG = "SurfaceProcessor"
    }

    private data class SurfaceInput(val surface: Surface, val surfaceTexture: SurfaceTexture)
}