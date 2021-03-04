package com.github.thibaultbee.streampack.encoders

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.gl.EGlSurface
import com.github.thibaultbee.streampack.gl.FullFrameRect
import com.github.thibaultbee.streampack.gl.Texture2DProgram
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.Logger
import com.github.thibaultbee.streampack.utils.getCameraOrientation
import java.nio.ByteBuffer
import java.security.InvalidParameterException
import java.util.concurrent.Executors

class VideoMediaCodecEncoder(
    encoderListener: IEncoderListener,
    override var onErrorListener: OnErrorListener?,
    private val context: Context,
    logger: Logger
) :
    MediaCodecEncoder(encoderListener, logger) {
    var codecSurface: CodecSurface? = null

    fun configure(videoConfig: VideoConfig) {
        val videoFormat = MediaFormat.createVideoFormat(
            videoConfig.mimeType,
            videoConfig.resolution.width,
            videoConfig.resolution.height
        )

        // Create codec
        val codec = createCodec(videoFormat)
        mediaCodec = codec

        // Extended video format
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoConfig.startBitrate)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoConfig.fps)
        videoFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1s between I frame
        /*  videoFormat.setInteger(MediaFormat.KEY_PROFILE, profile)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              videoFormat.setInteger(MediaFormat.KEY_LEVEL, level)
          }*/

        // Apply configuration
        mediaCodec?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                createHandler("VMediaCodecThread")
                it.setCallback(encoderCallback, handler)
            } else {
                it.setCallback(encoderCallback)
            }
            it.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            codecSurface = CodecSurface(it.createInputSurface(), context)
        } ?: throw InvalidParameterException("Can't start video MediaCodec")
    }

    override fun startStream() {
        codecSurface?.startStream()
        super.startStream()
    }

    override fun release() {
        super.release()
        codecSurface?.release()
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