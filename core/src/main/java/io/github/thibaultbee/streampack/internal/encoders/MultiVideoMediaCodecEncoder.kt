/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.internal.encoders

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.events.EventHandler
import io.github.thibaultbee.streampack.internal.gl.EglDisplayContext
import io.github.thibaultbee.streampack.internal.gl.EglWindowSurface
import io.github.thibaultbee.streampack.internal.gl.GlUtils
import io.github.thibaultbee.streampack.internal.gl.Texture2DProgram
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationListener
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.Logger
import java.util.concurrent.Executors

/**
 * Encoder for video using MediaCodec.
 *
 * @param orientationProvider to get the orientation of the source. If null, the source will keep its original dimensions.
 */
class MultiVideoMediaCodecEncoder(
    encoderListeners: List<IEncoderListener>,
    override val onInternalErrorListener: OnErrorListener,
    private val orientationProvider: ISourceOrientationProvider?,
    private val defaultBufferSize: Size = Size(1280, 720)
) : EventHandler(), ISourceOrientationListener, SurfaceTexture.OnFrameAvailableListener {

    private val targets: List<MultiVideoEncoderTargetInfo> = encoderListeners.map {
        MultiVideoEncoderTargetInfo(it)
    }
    private val eglDisplayContext = EglDisplayContext()
    private val executor = Executors.newSingleThreadExecutor()

    private data class Input(
        val program: Texture2DProgram,
        var textureId: Int,
        var surfaceTexture: SurfaceTexture,
        var surface: Surface
    )

    private var input: Input? = null
    private val stMatrix = FloatArray(16)

    val inputSurface: Surface?
        get() = input?.surface

    init {
        orientationProvider?.addListener(this)
        ensureInput()
    }

    private fun ensureInput() {
        if (input != null) {
            return
        }
        executor.submit {
            eglDisplayContext.makeCurrent()
            val program = Texture2DProgram()
            val textureId = program.createTextureObject()
            val surfaceTexture = SurfaceTexture(textureId)
            val surface = Surface(surfaceTexture)
            val size =
                orientationProvider?.getDefaultBufferSize(defaultBufferSize) ?: defaultBufferSize
            surfaceTexture.setDefaultBufferSize(size.width, size.height)
            surfaceTexture.setOnFrameAvailableListener(this)
            input = Input(program, textureId, surfaceTexture, surface)

        }.get()
    }

    private fun ensureGlContext(
        surface: EglWindowSurface,
        action: (EglWindowSurface) -> Unit
    ): EglWindowSurface {
        surface.makeCurrent()
        action(surface)
        surface.makeUnCurrent()
        return surface
    }

    override fun onOrientationChanged() {
        executor.execute {
            synchronized(this) {
                try {
                    targets.filter { it.eglSurface != null }.forEach { target ->
                        ensureGlContext(target.eglSurface!!) {
                            val width = it.getWidth()
                            val height = it.getHeight()

                            target.fullFrameRect?.setMVPMatrixAndViewPort(
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
                            input?.surfaceTexture?.updateTexImage()
                            input?.surfaceTexture?.releaseTexImage()
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "onOrientationChanged: ${e.message}")
                }
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (executor.isShutdown || executor.isTerminated) {
            Logger.e(
                TAG,
                "executor is shutdown(${executor.isShutdown}) or terminated(${executor.isTerminated})"
            )
        }
        executor.execute {
            synchronized(this) {
                val stMatrix = FloatArray(16)
                try {
                    val activeTargets = targets.filter { it.isActive }
                    if (activeTargets.isEmpty()) {
                        eglDisplayContext.makeCurrent()
                        surfaceTexture.updateTexImage()
                        eglDisplayContext.makeUnCurrent()
                    } else {
                        val first = activeTargets.first()
                        val last = activeTargets.last()
                        activeTargets.forEach { target ->
                            target.eglSurface?.let {
                                it.makeCurrent()
                                if (target == first) {
                                    surfaceTexture.updateTexImage()
                                    surfaceTexture.getTransformMatrix(stMatrix)
                                }
                                target.fullFrameRect?.drawFrame(input!!.textureId, stMatrix)
                               it.setPresentationTime(surfaceTexture.timestamp)
                                it.swapBuffers()
                                if (target == last) {
                                    surfaceTexture.releaseTexImage()
                                }
//                                it.makeUnCurrent()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "onFrameAvailable: ${e.message}")
                }
            }
        }
    }

    fun releaseTargets() {
        targets.forEach {
            if (it.isActive) {
                it.stop()
            }
            it.release()
        }
    }

    fun releaseInput() {
        orientationProvider?.removeListener(this)
        input?.let {
            it.surfaceTexture.setOnFrameAvailableListener(null)
            it.surfaceTexture.release()
            it.program.release()
            // other things for surface and texture id?
        }
        input = null
    }

    fun configure(encoderIndex: Int, config: VideoConfig) {
        ensureInput()
        targets[encoderIndex].configure(
            config,
            orientationProvider,
            executor,
            eglDisplayContext,
            input!!.program,
        )
    }

    fun startStream(encoderIndex: Int) {
        // copying code from VMCE.startStream to flush spurious frames
        executor.submit {
            synchronized(this) {
                eglDisplayContext.makeCurrent()
                input?.let { it.surfaceTexture.updateTexImage() }
                eglDisplayContext.makeUnCurrent()
            }
        }.get()
        targets[encoderIndex].start()
    }

    fun stopStream(encoderIndex: Int) {
        targets[encoderIndex].stop()
    }

    fun getTarget(encoderIndex: Int): MultiVideoEncoderTargetInfo {
        return targets[encoderIndex]
    }

    companion object {
        const val TAG = "MultiVideoMediaCodecEncoder"
    }
}
