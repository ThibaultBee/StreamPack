package io.github.thibaultbee.streampack.internal.encoders

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.gl.EglDisplayContext
import io.github.thibaultbee.streampack.internal.gl.EglWindowSurface
import io.github.thibaultbee.streampack.internal.gl.FullFrameRect
import io.github.thibaultbee.streampack.internal.gl.Texture2DProgram
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider
import io.github.thibaultbee.streampack.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IVideoSettings
import java.util.concurrent.ExecutorService

data class MultiVideoEncoderTargetInfo(
    val listener: IEncoderListener,
    private var _bitrate: Int? = null,
    var mediaCodec: MediaCodec? = null,
    var useHighBitDepth: Boolean = false,
    var encoderInputSurface: Surface? = null,
    var isActive: Boolean = false,
    var eglSurface: EglWindowSurface? = null,
    var fullFrameRect: FullFrameRect? = null,
    private val lock: Any = Object(),
    private var isOnError: Boolean = false,
    private var callbackThread: HandlerThread? = null,
    private var handler: Handler? = null
) : IVideoSettings {
    private val encoderCallback = object : MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            synchronized(lock) {
                if (!isActive) {
                    return
                }
                if (isOnError) {
                    return
                }
                try {
                    mediaCodec?.getOutputBuffer(index)?.let { buffer ->
                        val format = codec.outputFormat
                        val isKeyFrame = info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
                        /**
                         * Drops codec data. They are already passed in the extra buffer.
                         */
                        if (info.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            Frame(
                                buffer,
                                info.presentationTimeUs, // pts
                                null, // dts
                                isKeyFrame,
                                format
                            ).let { frame ->
                                listener.onOutputFrame(
                                    frame
                                )
                            }
                        }

                        mediaCodec?.releaseOutputBuffer(index, false)
                    }
                        ?: reportError(StreamPackError(UnsupportedOperationException("MediaCodecEncoder: can't get output buffer")))
                } catch (e: IllegalStateException) {
                    isOnError = true
                    Logger.w(
                        MultiVideoMediaCodecEncoder.TAG,
                        "onOutputBufferAvailable called while stopped"
                    )
                } catch (e: StreamPackError) {
                    isOnError = true
                    reportError(e)
                }
            }
        }

        private fun reportError(err: StreamPackError) =
            // NOT YET IMPLEMENTED : report the error
            Unit

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            /**
             * An IllegalStateException happens when MediaCodec is stopped. Dirty fix: catch it...
             */
            synchronized(lock) {
                if (!isActive) {
                    return
                }
                if (isOnError) {
                    return
                }

                try {
                    mediaCodec?.getInputBuffer(index)?.let { buffer ->
                        listener.onInputFrame(buffer).let { frame ->
                            mediaCodec?.queueInputBuffer(
                                index,
                                0,
                                frame.buffer.remaining(),
                                frame.pts /* in us */,
                                0
                            )
                        }
                    }
                        ?: reportError(
                            StreamPackError(
                                UnsupportedOperationException("MediaCodecEncoder: can't get input buffer")
                            )
                        )
                } catch (e: IllegalStateException) {
                    isOnError = true
                    Logger.w(
                        MultiVideoMediaCodecEncoder.TAG,
                        "onInputBufferAvailable called while stopped"
                    )
                } catch (e: StreamPackError) {
                    isOnError = true
                    reportError(e)
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Logger.i(MultiVideoMediaCodecEncoder.TAG, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Logger.e(MultiVideoMediaCodecEncoder.TAG, "MediaCodecCallback.onError(${e.message})")
            reportError(StreamPackError(e))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiVideoEncoderTargetInfo

        return listener == other.listener
    }

    override fun hashCode(): Int {
        return listener.hashCode()
    }

    override var bitrate: Int
        get() = _bitrate ?: 0
        set(value) {
            val bundle = Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, value)
            mediaCodec?.setParameters(bundle)
            _bitrate = value
        }

    private fun createHandler(name: String) {
        callbackThread = HandlerThread(name)
        handler = callbackThread?.let { handlerThread ->
            handlerThread.start()
            Handler(handlerThread.looper)
        }
    }

    private fun releaseHandler() {
        handler = null
        callbackThread?.quitSafely()
        callbackThread = null
    }

    fun configure(
        config: VideoConfig,
        orientationProvider: ISourceOrientationProvider?,
        executor: ExecutorService,
        displayContext: EglDisplayContext,
        program: Texture2DProgram,
    ) {
        _bitrate = config.startBitrate
        val mediaFormat = config.getFormat(true)
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        val encoderName = MediaCodecHelper.findEncoder(mediaFormat)
        // createCodec
        mediaCodec = MediaCodec.createByCodecName(encoderName)

        val codec = mediaCodec!!

        // this replicates weird workaround in MediaCodecEncoder.createCodec
        val dims = orientationProvider?.getOrientedSize(config.resolution) ?: config.resolution
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, dims.width)
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, dims.height)

        // Apply configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createHandler("$encoderName.thread")
            codec.setCallback(encoderCallback, handler)
        } else {
            codec.setCallback(encoderCallback)
        }

        try {
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            codec.release()
            throw e
        }
        useHighBitDepth = try {
            val mimeType = codec.outputFormat.getString(MediaFormat.KEY_MIME)!!
            val profile = codec.outputFormat.getInteger(MediaFormat.KEY_PROFILE)
            DynamicRangeProfile.fromProfile(mimeType, profile).isHdr
        } catch (_: Exception) {
            false
        }

        // create input surface
        encoderInputSurface = codec.createInputSurface()
        executor.submit {
            eglSurface =
                EglWindowSurface(encoderInputSurface!!, useHighBitDepth, displayContext)
            eglSurface?.let {
                it.makeCurrent()
                val width = it.getWidth()
                val height = it.getHeight()
                val size =
                    orientationProvider?.getOrientedSize(Size(width, height)) ?: Size(
                        width,
                        height
                    )
                val orientation = orientationProvider?.orientation ?: 0
                fullFrameRect = FullFrameRect(program).apply {
                    setMVPMatrixAndViewPort(
                        orientation.toFloat(),
                        size,
                        orientationProvider?.mirroredVertically ?: false
                    )
                }
                it.makeUnCurrent()
            }
        }.get()
    }

    fun start() {
        isActive = true
        mediaCodec?.start()
    }

    fun stop() {
        try {
            synchronized(lock) {
                if (!isActive) {
                    return
                }
                isActive = false
                mediaCodec?.signalEndOfInputStream()
                mediaCodec?.flush()
                mediaCodec?.stop()
                mediaCodec?.setCallback(null)
                releaseHandler() // prevent thread leak
            }
        } catch (e: IllegalStateException) {
            Logger.d(MultiVideoMediaCodecEncoder.TAG, "Not running")
        }
    }

    fun release() {
        stop()
        fullFrameRect?.release(false)
        eglSurface?.release()
        eglSurface = null
        fullFrameRect = null
        encoderInputSurface = null
    }
}