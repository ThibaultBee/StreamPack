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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.gl.EGlSurface
import io.github.thibaultbee.streampack.internal.gl.FullFrameRect
import io.github.thibaultbee.streampack.internal.gl.Texture2DProgram
import io.github.thibaultbee.streampack.internal.utils.getOrientation
import io.github.thibaultbee.streampack.internal.utils.isPortrait
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.ILogger
import java.util.concurrent.Executors

class VideoMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override val onInternalErrorListener: OnErrorListener,
    private val context: Context,
    private val manageVideoOrientation: Boolean,
    logger: ILogger
) :
    MediaCodecEncoder<VideoConfig>(encoderListener, logger) {
    var codecSurface = CodecSurface(context, manageVideoOrientation)

    override fun configure(config: VideoConfig) {
        mediaCodec = try {
            createVideoCodec(config, true)
        } catch (e: MediaCodec.CodecException) {
            createVideoCodec(config, false)
        }.apply { codecSurface.surface = createInputSurface() }
    }

    private fun createVideoCodec(
        videoConfig: VideoConfig,
        useConfigProfileLevel: Boolean
    ): MediaCodec {
        val resolution = if (manageVideoOrientation) {
            videoConfig.getOrientedResolution(context)
        } else {
            videoConfig.resolution
        }
        val videoFormat = MediaFormat.createVideoFormat(
            videoConfig.mimeType,
            resolution.width,
            resolution.height
        )

        // Create codec
        val codec = createCodec(videoFormat)

        // Extended video format
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoConfig.startBitrate)
        _bitrate = videoConfig.startBitrate

        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoConfig.fps)
        videoFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1s between I frame

        if (useConfigProfileLevel) {
            videoFormat.setInteger(MediaFormat.KEY_PROFILE, videoConfig.profile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                videoFormat.setInteger(MediaFormat.KEY_LEVEL, videoConfig.level)
            }
        }

        // Apply configuration
        configureCodec(codec, videoFormat, "VMediaCodecThread")
        return codec
    }

    override fun startStream() {
        codecSurface.startStream()
        super.startStream()
    }

    override fun stopStream() {
        codecSurface.stopStream()
        super.stopStream()
    }

    val inputSurface: Surface?
        get() = codecSurface.inputSurface

    class CodecSurface(private val context: Context, private val manageVideoOrientation: Boolean) :
        SurfaceTexture.OnFrameAvailableListener {
        private var eglSurface: EGlSurface? = null
        private var fullFrameRect: FullFrameRect? = null
        private var textureId = -1
        private val executor = Executors.newSingleThreadExecutor()
        private var isRunning = false
        private var surfaceTexture: SurfaceTexture? = null
        val inputSurface: Surface?
            get() = surfaceTexture?.let { Surface(surfaceTexture) }

        var surface: Surface? = null
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

        private fun initOrUpdateSurfaceTexture(surface: Surface) {
            eglSurface = ensureGlContext(EGlSurface(surface)) {
                val width = it.getWidth()
                val height = it.getHeight()
                fullFrameRect = FullFrameRect(Texture2DProgram()).apply {
                    textureId = createTextureObject()
                    setMVPMatrixAndViewPort(
                        context.getOrientation().toFloat(),
                        Size(width, height)
                    )
                }

                surfaceTexture = attachOrBuildSurfaceTexture(surfaceTexture).apply {
                    if (manageVideoOrientation) {
                        if (context.isPortrait()) {
                            setDefaultBufferSize(height, width)
                        } else {
                            setDefaultBufferSize(width, height)
                        }
                    } else {
                        setDefaultBufferSize(width, height)
                    }
                    setOnFrameAvailableListener(this@CodecSurface)
                }
            }
        }

        @SuppressLint("Recycle")
        private fun attachOrBuildSurfaceTexture(surfaceTexture: SurfaceTexture?): SurfaceTexture {
            return if (surfaceTexture == null) {
                SurfaceTexture(textureId)
            } else {
                surfaceTexture.attachToGLContext(textureId)
                surfaceTexture
            }
        }

        private fun ensureGlContext(
            surface: EGlSurface?,
            action: (EGlSurface) -> Unit
        ): EGlSurface? {
            surface?.let {
                it.makeCurrent()
                action(it)
                it.makeUnCurrent()
            }
            return surface
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            synchronized(this) {
                if (!isRunning) {
                    return
                }

                executor.execute {
                    eglSurface?.let {
                        it.makeCurrent()
                        surfaceTexture.updateTexImage()
                        val stMatrix = FloatArray(16)
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

        fun dispose() {
            stopStream()
            surfaceTexture?.release()
            surfaceTexture = null
        }
    }
}
