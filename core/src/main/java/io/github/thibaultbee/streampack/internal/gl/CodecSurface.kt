package io.github.thibaultbee.streampack.internal.gl

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationListener
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider
import java.util.concurrent.Executors

class CodecSurface(
    private val orientationProvider: ISourceOrientationProvider?
) :
    SurfaceTexture.OnFrameAvailableListener, ISourceOrientationListener {
    private val executor = Executors.newSingleThreadExecutor()

    private var eglSurface: EglWindowSurface? = null
    private var fullFrameRect: FullFrameRect? = null
    private var textureId = -1

    private var isRunning = false
    private var surfaceTexture: SurfaceTexture? = null
    private val stMatrix = FloatArray(16)

    private var _inputSurface: Surface? = null

    val input: Surface?
        get() = _inputSurface

    /**
     * If true, the encoder will use high bit depth (10 bits) for encoding.
     */
    var useHighBitDepth = false

    var outputSurface: Surface? = null
        set(value) {
            /**
             * When surface is called twice without the stopStream(). When configure() is
             * called twice for example,
             */
            executor.submit {
                if (eglSurface != null) {
                    detachSurfaceTexture()
                }
                synchronized(this) {
                    value?.let {
                        initOrUpdateSurfaceTexture(it)
                    }
                }

            }.get() // Wait till executor returns
            field = value
        }

    init {
        orientationProvider?.addListener(this)
    }

    private fun initOrUpdateSurfaceTexture(surface: Surface) {
        eglSurface = ensureGlContext(EglWindowSurface(surface, useHighBitDepth)) {
            val width = it.getWidth()
            val height = it.getHeight()
            val size =
                orientationProvider?.getOrientedSize(Size(width, height)) ?: Size(width, height)
            val orientation = orientationProvider?.orientation ?: 0
            fullFrameRect = FullFrameRect(Texture2DProgram()).apply {
                textureId = createTextureObject()
                setMVPMatrixAndViewPort(
                    orientation.toFloat(),
                    size,
                    orientationProvider?.mirroredVertically ?: false
                )
            }

            val defaultBufferSize =
                orientationProvider?.getDefaultBufferSize(size) ?: Size(width, height)
            surfaceTexture = attachOrBuildSurfaceTexture(surfaceTexture).apply {
                setDefaultBufferSize(defaultBufferSize.width, defaultBufferSize.height)
                setOnFrameAvailableListener(this@CodecSurface)
            }
        }
    }

    @SuppressLint("Recycle")
    private fun attachOrBuildSurfaceTexture(surfaceTexture: SurfaceTexture?): SurfaceTexture {
        return if (surfaceTexture == null) {
            SurfaceTexture(textureId).apply {
                _inputSurface = Surface(this)
            }
        } else {
            surfaceTexture.attachToGLContext(textureId)
            surfaceTexture
        }
    }

    private fun ensureGlContext(
        surface: EglWindowSurface?,
        action: (EglWindowSurface) -> Unit
    ): EglWindowSurface? {
        surface?.let {
            it.makeCurrent()
            action(it)
            it.makeUnCurrent()
        }
        return surface
    }

    override fun onOrientationChanged() {
        executor.execute {
            synchronized(this) {
                ensureGlContext(eglSurface) {
                    val width = it.getWidth()
                    val height = it.getHeight()

                    fullFrameRect?.setMVPMatrixAndViewPort(
                        (orientationProvider?.orientation ?: 0).toFloat(),
                        orientationProvider?.getOrientedSize(Size(width, height)) ?: Size(
                            width,
                            height
                        ),
                        orientationProvider?.mirroredVertically ?: false
                    )

                    /**
                     * Flushing spurious latest camera frames that block SurfaceTexture buffer
                     * to avoid having a misoriented frame.
                     */
                    surfaceTexture?.updateTexImage()
                    surfaceTexture?.releaseTexImage()
                }
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (!isRunning) {
            return
        }

        executor.execute {
            synchronized(this) {
                eglSurface?.let {
                    it.makeCurrent()
                    surfaceTexture.updateTexImage()
                    surfaceTexture.getTransformMatrix(stMatrix)

                    // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
                    fullFrameRect?.drawFrame(textureId, stMatrix)
                    it.setPresentationTime(surfaceTexture.timestamp)
                    it.swapBuffers()
                    surfaceTexture.releaseTexImage()
                }
            }
        }
    }

    fun startStream() {
        // Flushing spurious latest camera frames that block SurfaceTexture buffer.
        ensureGlContext(eglSurface) {
            surfaceTexture?.updateTexImage()
        }
        isRunning = true
    }

    private fun detachSurfaceTexture() {
        ensureGlContext(eglSurface) {
            surfaceTexture?.detachFromGLContext()
            fullFrameRect?.release(true)
        }
        eglSurface?.release()
        eglSurface = null
        fullFrameRect = null
    }

    fun stopStream() {
        executor.submit {
            synchronized(this) {
                isRunning = false
                detachSurfaceTexture()
            }
        }.get()
    }

    fun release() {
        orientationProvider?.removeListener(this)
        stopStream()
        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        surfaceTexture = null
    }
}