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
package com.github.thibaultbee.streampack.internal.encoders

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.gl.EGlSurface
import com.github.thibaultbee.streampack.internal.gl.FullFrameRect
import com.github.thibaultbee.streampack.internal.gl.Texture2DProgram
import com.github.thibaultbee.streampack.internal.sources.camera.getCameraOrientation
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.logger.ILogger
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class VideoMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override val onInternalErrorListener: OnErrorListener,
    private val context: Context,
    logger: ILogger
) :
    MediaCodecEncoder<VideoConfig>(encoderListener, logger) {
    var codecSurface: CodecSurface? = null

    override fun configure(config: VideoConfig) {
        mediaCodec = try {
            createVideoCodec(config, true)
        } catch (e: MediaCodec.CodecException) {
            createVideoCodec(config, false)
        }
    }

    private fun createVideoCodec(
        videoConfig: VideoConfig,
        useConfigProfileLevel: Boolean
    ): MediaCodec {
        val videoFormat = MediaFormat.createVideoFormat(
            videoConfig.mimeType,
            videoConfig.resolution.width,
            videoConfig.resolution.height
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createHandler("VMediaCodecThread")
            codec.setCallback(encoderCallback, handler)
        } else {
            codec.setCallback(encoderCallback)
        }
        try {
            codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codecSurface = CodecSurface(codec.createInputSurface(), context)
            return codec
        } catch (e: Exception) {
            codec.release()
            throw e
        }
    }

    override fun startStream() {
        codecSurface?.startStream()
        super.startStream()
    }

    override fun release() {
        super.release()
        codecSurface?.release()
        codecSurface = null
    }

    override fun onGenerateExtra(buffer: ByteBuffer, format: MediaFormat): ByteBuffer {
        val csd0 = format.getByteBuffer("csd-0")
        val csd1 = format.getByteBuffer("csd-1")

        var byteBufferSize = csd0?.limit() ?: 0
        byteBufferSize += csd1?.limit() ?: 0

        val extra = ByteBuffer.allocate(byteBufferSize)
        csd0?.let { extra.put(it) }
        csd1?.let { extra.put(it) }

        extra.rewind()
        return extra
    }

    val inputSurface: Surface
        get() = Surface(codecSurface?.surfaceTexture)

    class CodecSurface(surface: Surface, private val context: Context) :
        SurfaceTexture.OnFrameAvailableListener {
        private val eglSurface = EGlSurface(surface)
        private val textureId: Int
        private val fullFrameRect: FullFrameRect
        private val executor = Executors.newSingleThreadExecutor()
        private var isReleased = false

        val surfaceTexture: SurfaceTexture

        init {
            eglSurface.makeCurrent()
            fullFrameRect = FullFrameRect(Texture2DProgram())
            textureId = fullFrameRect.createTextureObject()
            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture.setDefaultBufferSize(eglSurface.getWidth(), eglSurface.getHeight())
            surfaceTexture.setOnFrameAvailableListener(this)
            eglSurface.makeUnCurrent()
        }


        fun startStream() {
            fullFrameRect.setMVPMatrixAndViewPort(
                context.getCameraOrientation().toFloat(),
                Size(eglSurface.getWidth(), eglSurface.getHeight())
            )
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            synchronized(this) {
                if (isReleased) {
                    return
                }

                executor.execute {
                    eglSurface.makeCurrent()
                    surfaceTexture.updateTexImage()
                    val stMatrix = FloatArray(16)
                    surfaceTexture.getTransformMatrix(stMatrix)

                    // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
                    fullFrameRect.drawFrame(textureId, stMatrix)
                    eglSurface.setPresentationTime(surfaceTexture.timestamp)
                    eglSurface.swapBuffers()
                    surfaceTexture.releaseTexImage()
                }
            }
        }

        fun release() {
            synchronized(this) {
                isReleased = true
                fullFrameRect.release(true)
                eglSurface.release()
                surfaceTexture.release()
            }
        }
    }
}