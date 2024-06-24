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
package io.github.thibaultbee.streampack.core.internal.encoders.mediacodec

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodec.CodecException
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import io.github.thibaultbee.streampack.core.error.StreamPackError
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.encoders.IEncoder
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Creates a [MediaCodec] encoder with a given configuration and a listener.
 */
internal fun MediaCodecEncoder(
    encoderConfig: EncoderConfig<*>,
    listener: IEncoder.IListener,
    encoderExecutor: Executor = Executors.newSingleThreadExecutor(),
    listenerExecutor: Executor = Executors.newSingleThreadExecutor(),
): MediaCodecEncoder {
    return MediaCodecEncoder(
        encoderConfig,
        encoderExecutor
    ).apply { setListener(listener, listenerExecutor) }
}

/**
 * The [MediaCodec] encoder implementation.
 *
 * @param encoderConfig the encoder configuration
 * @param encoderExecutor the executor to run the encoder. Must be a single thread executor. Is only visible for testing.
 */
class MediaCodecEncoder
internal constructor(
    private val encoderConfig: EncoderConfig<*>,
    private val encoderExecutor: Executor = Executors.newSingleThreadExecutor()
) :
    IEncoder {
    private val mediaCodec: MediaCodec
    private val format: MediaFormat
    private val isVideo = encoderConfig.isVideo
    private val tag = if (isVideo) "VideoEncoder" else "AudioEncoder"

    private val dispatcher = encoderExecutor.asCoroutineDispatcher()

    private var isStopped = true
    private var isOnError = false
    private var isReleased = false

    override val startBitrate = encoderConfig.config.startBitrate
    override var bitrate: Int = startBitrate
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Bitrate must be positive")
            }
            if (isVideo) {
                val bundle = Bundle()
                bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, value)
                mediaCodec.setParameters(bundle)
                field = value
            } else {
                throw UnsupportedOperationException("Audio encoder does not support bitrate change")
            }
        }


    private val encoderCallback = EncoderCallback()

    init {
        val mediaCodecWithFormat = MediaCodecUtils.createCodec(encoderConfig)
        mediaCodec = mediaCodecWithFormat.mediaCodec
        format = mediaCodecWithFormat.format
    }

    private val listenerLock = Any()
    private var listener: IEncoder.IListener =
        object : IEncoder.IListener {}
    private var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    override val mimeType = format.getString(MediaFormat.KEY_MIME)!!

    override val input =
        if (encoderConfig is VideoEncoderConfig) {
            if (encoderConfig.useSurfaceMode) {
                SurfaceInput()
            } else {
                ByteBufferInput()
            }
        } else {
            ByteBufferInput()
        }

    override val info = EncoderInfo.build(mediaCodec.codecInfo, mimeType)


    override fun requestKeyFrame() {
        val bundle = Bundle()
        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
        mediaCodec.setParameters(bundle)
    }

    override fun setListener(
        listener: IEncoder.IListener,
        listenerExecutor: Executor
    ) {
        synchronized(listenerLock) {
            this.listenerExecutor = listenerExecutor
            this.listener = listener
        }
    }

    override fun configure() {
        /**
         * This is a workaround because few Samsung devices (such as Samsung Galaxy J7 Prime does
         * not find any encoder if the width and height are oriented to portrait.
         * We defer orientation of width and height to here.
         */
        if (encoderConfig is VideoEncoderConfig) {
            encoderConfig.orientateFormat(format)
        }

        try {
            /**
             * Set encoder callback without handler.
             * The [encoderExecutor] is used to run the encoder callbacks.
             */
            mediaCodec.setCallback(encoderCallback)

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            if (input is SurfaceInput) {
                input.reset()
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to configure for format: $format", e)
            release()
            throw e
        }
    }

    private fun resetSync() {
        try {
            mediaCodec.reset()
        } catch (e: IllegalStateException) {
            Logger.d(tag, "Failed to reset")
        } finally {
            isOnError = false
        }
        configure()
    }

    override fun reset() {
        encoderExecutor.execute {
            resetSync()
        }
    }

    private fun startStreamSync() {
        if (!isStopped) {
            return
        }
        try {
            mediaCodec.start()
        } catch (e: IllegalStateException) {
            Logger.d(tag, "Not running")
        } finally {
            isStopped = false
        }
    }

    override suspend fun startStream() {
        withContext(dispatcher) {
            startStreamSync()
        }
    }

    private fun stopStreamSync() {
        if (isStopped) {
            return
        }
        try {
            if (input is IEncoder.ISurfaceInput) {
                // If we stop the codec, then it will stop de-queuing
                // buffers and the BufferQueue may run out of input buffers, causing the camera
                // pipeline to stall. Instead of stopping, we will flush the codec.
                mediaCodec.flush()
            } else {
                mediaCodec.stop()
            }

        } catch (e: IllegalStateException) {
            Logger.d(tag, "Not running")
        } finally {
            isStopped = true
        }
    }

    override suspend fun stopStream() {
        withContext(dispatcher) {
            stopStreamSync()
        }
    }

    private fun releaseSync() {
        if (isReleased) {
            return
        }
        try {
            mediaCodec.release()

            if (input is SurfaceInput) {
                input.release()
            }
        } catch (_: Exception) {
        } finally {
            isReleased = true
        }
    }

    override fun release() {
        runBlocking {
            if (!isStopped) {
                stopStream()
            }
            withContext(dispatcher) {
                releaseSync()
            }
        }
    }

    private fun notifyError(e: Exception) {
        var listener: IEncoder.IListener
        var listenerExecutor: Executor
        synchronized(listenerLock) {
            listener = this.listener
            listenerExecutor = this.listenerExecutor
        }
        listenerExecutor.execute {
            listener.onError(e)
        }
    }

    private fun handleError(e: Exception) {
        isOnError = true
        if (!isStopped) {
            encoderExecutor.execute {
                stopStreamSync()
                notifyError(e)
            }
        } else {
            notifyError(e)
        }
    }

    private fun reachEndOfStream() {
        runBlocking {
            stopStream()
        }
    }

    private inner class EncoderCallback : MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: BufferInfo
        ) {
            encoderExecutor.execute {
                if (isStopped) {
                    Logger.w(tag, "Receives frame after codec is reset.")
                    return@execute
                }
                if (isOnError) {
                    return@execute
                }

                if (isBufferInfoValid(info, tag)) {
                    try {
                        var listener: IEncoder.IListener
                        var listenerExecutor: Executor
                        synchronized(listenerLock) {
                            listener = this@MediaCodecEncoder.listener
                            listenerExecutor = this@MediaCodecEncoder.listenerExecutor
                        }

                        try {
                            Frame(
                                codec,
                                index,
                                info
                            ).let { frame ->
                                listenerExecutor.execute {
                                    try {
                                        listener.onOutputFrame(frame)
                                    } catch (e: Exception) {
                                        handleError(e)
                                    } finally {
                                        try {
                                            codec.releaseOutputBuffer(index, false)
                                        } catch (e: java.lang.IllegalStateException) {
                                            Logger.w(tag, "Failed to release output buffer", e)
                                        }
                                    }
                                }
                            }
                        } catch (e: CodecException) {
                            handleError(e)
                        }
                    } catch (e: CodecException) {
                        handleError(e)
                    }
                } else {
                    try {
                        codec.releaseOutputBuffer(index, false)
                    } catch (e: CodecException) {
                        handleError(e)
                    }
                }

                if (hasEndOfStreamFlag(info)) {
                    reachEndOfStream()
                }
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            encoderExecutor.execute {
                if (isStopped) {
                    Logger.w(tag, "Receives input frame after codec is reset.")
                    return@execute
                }
                if (isOnError) {
                    return@execute
                }

                if (input !is IEncoder.IByteBufferInput) {
                    Logger.w(tag, "Input buffer is only available for byte buffer input")
                    return@execute
                }

                try {
                    val buffer = mediaCodec.getInputBuffer(index)
                        ?: throw UnsupportedOperationException("MediaCodecEncoder: can't get input buffer")
                    val frame = input.listener.onFrameRequested(buffer)
                    queueInputFrame(index, frame)
                } catch (e: Exception) {
                    handleError(e)
                } catch (e: StreamPackError) {
                    handleError(e)
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Logger.i(tag, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: CodecException) {
            handleError(StreamPackError(e))
        }

        private fun queueInputFrame(
            index: Int,
            frame: Frame
        ) {
            mediaCodec.queueInputBuffer(
                index,
                frame.buffer.position(),
                frame.buffer.limit(),
                frame.pts /* in us */,
                0
            )
        }
    }

    internal inner class SurfaceInput :
        IEncoder.ISurfaceInput {
        private val obsoleteSurfaces = mutableListOf<Surface>()

        private var surface: Surface? = null

        override var listener = object :
            IEncoder.ISurfaceInput.OnSurfaceUpdateListener {}
            set(value) {
                field = value
                surface?.let { notifySurfaceUpdate(it) }
            }

        fun reset() {
            val surface = synchronized(this) {
                surface?.let {
                    obsoleteSurfaces.add(it)
                }
                this.surface = mediaCodec.createInputSurface()
                this.surface
            }
            surface?.let { notifySurfaceUpdate(it) }
        }


        /**
         * Releases the surface
         */
        fun release() {
            val surface = synchronized(this) {
                this.surface
            }
            surface?.release()
            obsoleteSurfaces.forEach { it.release() }
        }

        private fun notifySurfaceUpdate(surface: Surface) {
            listener.onSurfaceUpdated(surface)
        }
    }

    internal inner class ByteBufferInput :
        IEncoder.IByteBufferInput {
        override lateinit var listener: IEncoder.IByteBufferInput.OnFrameRequestedListener
    }

    companion object {
        private fun isBufferInfoValid(info: BufferInfo, tag: String): Boolean {
            if (info.size <= 0) {
                Logger.w(tag, "Invalid buffer size: ${info.size}")
                return false
            }

            if (isCodecConfig(info)) {
                Logger.d(tag, "Drop buffer by codec config.")
                return false
            }
            return true
        }

        /**
         * Whether if the buffer is a codec config buffer
         *
         * @param info the buffer info
         * @return true if the buffer is a codec config buffer
         */
        private fun isCodecConfig(info: BufferInfo) =
            info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

        /**
         * Whether if the buffer is a key frame
         *
         * @param info the buffer info
         * @return true if the buffer is a key frame
         */
        private fun isKeyFrame(info: BufferInfo) =
            info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0

        /**
         * Whether if the buffer is an end of stream buffer
         *
         * @param info the buffer info
         * @return true if the buffer is an end of stream buffer
         */
        private fun hasEndOfStreamFlag(info: BufferInfo) =
            info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0

        /**
         * Create a [Frame] from a [MediaCodec] output buffer
         *
         * @param mediaCodec the [MediaCodec] instance
         * @param index the buffer index
         * @param info the buffer info
         */
        private fun Frame(
            mediaCodec: MediaCodec,
            index: Int,
            info: BufferInfo
        ): Frame {
            val buffer = mediaCodec.getOutputBuffer(index)
                ?: throw UnsupportedOperationException("MediaCodecEncoder: can't get output buffer for index $index")
            return Frame(
                buffer,
                info.presentationTimeUs, // pts
                null, // dts
                isKeyFrame(info),
                mediaCodec.outputFormat
            )
        }
    }
}
