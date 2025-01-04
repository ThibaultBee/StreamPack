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
package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodec.CodecException
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.EncoderMode
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.hasEndOfStreamFlag
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.isKeyFrame
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.isValid
import io.github.thibaultbee.streampack.core.elements.utils.extensions.put
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Creates a [MediaCodec] encoder with a given configuration and a listener.
 */
internal fun MediaCodecEncoder(
    encoderConfig: EncoderConfig<*>,
    listener: IEncoderInternal.IListener,
    encoderExecutor: Executor = Executors.newSingleThreadExecutor(),
    listenerExecutor: Executor = Executors.newSingleThreadExecutor(),
): MediaCodecEncoder {
    return MediaCodecEncoder(
        encoderConfig, encoderExecutor
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
) : IEncoderInternal {
    private val mediaCodec: MediaCodec
    private val format: MediaFormat
    private val isVideo = encoderConfig.isVideo
    private val tag = if (isVideo) "VideoEncoder" else "AudioEncoder" + "(${this.hashCode()})"

    private val dispatcher = encoderExecutor.asCoroutineDispatcher()

    private var state = State.IDLE

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

    override val config = encoderConfig.config

    private val encoderCallback = EncoderCallback()

    init {
        val mediaCodecWithFormat = MediaCodecUtils.createCodec(encoderConfig)
        mediaCodec = mediaCodecWithFormat.mediaCodec
        format = mediaCodecWithFormat.format
    }

    private val listenerLock = Any()
    private var listener: IEncoderInternal.IListener = object : IEncoderInternal.IListener {}
    private var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    override val mimeType by lazy { format.getString(MediaFormat.KEY_MIME)!! }

    override val input = when (encoderConfig.mode) {
        EncoderMode.SURFACE -> SurfaceInput()
        EncoderMode.SYNC -> SyncByteBufferInput()
        EncoderMode.ASYNC -> AsyncByteBufferInput()
    }

    override val info by lazy { EncoderInfo.build(mediaCodec.codecInfo, mimeType) }

    override fun requestKeyFrame() {
        val bundle = Bundle()
        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
        mediaCodec.setParameters(bundle)
    }

    override fun setListener(
        listener: IEncoderInternal.IListener, listenerExecutor: Executor
    ) {
        synchronized(listenerLock) {
            this.listenerExecutor = listenerExecutor
            this.listener = listener
        }
    }

    override fun configure() {
        try {
            if (input !is SyncByteBufferInput) {
                /**
                 * Set encoder callback without handler.
                 * The [encoderExecutor] is used to run the encoder callbacks.
                 */
                mediaCodec.setCallback(encoderCallback)
            }

            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            if (input is SurfaceInput) {
                input.reset()
            }
            setState(State.CONFIGURED)
        } catch (t: Throwable) {
            Logger.e(tag, "Failed to configure for format: $format", t)
            release()
            throw t
        }
    }

    private fun resetSync() {
        if (state == State.CONFIGURED) {
            return
        }

        try {
            mediaCodec.reset()
        } catch (e: IllegalStateException) {
            Logger.d(tag, "Failed to reset")
        } finally {
            configure()
        }
    }

    override fun reset() {
        encoderExecutor.execute {
            resetSync()
        }
    }

    private fun startStreamSync() {
        when (state) {
            State.STARTED, State.PENDING_START, State.ERROR -> {
                return
            }

            State.PENDING_RELEASE, State.RELEASED -> {
                throw IllegalStateException("Encoder is released")
            }

            State.CONFIGURED -> {
                setState(State.PENDING_START)
                try {
                    mediaCodec.start()
                    setState(State.STARTED)
                } catch (e: CodecException) {
                    setState(State.ERROR)
                    throw e
                }
            }

            else -> {
            }
        }
    }

    override suspend fun startStream() {
        withContext(dispatcher) {
            startStreamSync()
        }
    }

    private fun stopStreamSync() {
        when (state) {
            State.PENDING_STOP, State.STOPPED, State.ERROR -> {
                return
            }

            State.RELEASED, State.PENDING_RELEASE -> {
                throw IllegalStateException("Encoder is released")
            }

            State.STARTED, State.PAUSED -> {
                setState(State.PENDING_STOP)
                try {
                    if (input is IEncoderInternal.ISurfaceInput) {
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
                    setState(State.STOPPED)
                }
            }

            else -> {
            }
        }
    }

    override suspend fun stopStream() {
        withContext(dispatcher) {
            stopStreamSync()
        }
    }

    private fun releaseSync() {
        try {
            mediaCodec.stop()
        } catch (e: IllegalStateException) {
            Logger.d(tag, "Failed to stop")
        }
        if (state == State.RELEASED || state == State.PENDING_RELEASE) {
            return
        }

        setState(State.PENDING_RELEASE)
        try {
            mediaCodec.release()

            if (input is SurfaceInput) {
                input.release()
            }
        } catch (_: Throwable) {
        } finally {
            setState(State.RELEASED)
        }
    }

    override fun release() {
        runBlocking {
            withContext(dispatcher) {
                releaseSync()
            }
        }
    }

    private fun notifyError(t: Throwable) {
        var listener: IEncoderInternal.IListener
        var listenerExecutor: Executor
        synchronized(listenerLock) {
            listener = this.listener
            listenerExecutor = this.listenerExecutor
        }
        listenerExecutor.execute {
            listener.onError(t)
        }
    }

    private fun handleError(t: Throwable) {
        encoderExecutor.execute {
            when (state) {
                State.CONFIGURED -> {
                    notifyError(t)
                    // Trying to reset
                    reset()
                }

                State.RELEASED -> { // Do Nothing
                }

                State.PENDING_STOP -> { // Do Nothing
                }

                State.STOPPED -> { // Do Nothing
                }

                State.ERROR -> {
                    Logger.w(tag, "Get another error while in error state: ${t.message}", t)
                }

                else -> {
                    try {
                        stopStreamSync()
                    } catch (e: Throwable) {
                        Logger.w(tag, "Failed to stop stream", e)
                    }
                    setState(State.ERROR)
                    notifyError(t)
                }
            }
        }
    }

    private fun reachEndOfStream() {
        runBlocking {
            stopStream()
        }
    }

    private fun setState(state: State) {
        if (state == this.state) {
            return
        }
        Logger.d(tag, "Transitioning encoder internal state: ${this.state} --> $state")
        this.state = state
    }

    private fun processOutputFrameSync(codec: MediaCodec, index: Int, info: BufferInfo) {
        when {
            !state.isRunning -> {
                Logger.w(tag, "Receives output frame after codec is not running: $state.")
                try {
                    codec.releaseOutputBuffer(index, false)
                } catch (_: Throwable) {
                    // Do nothing
                }
                return
            }

            info.isValid -> {
                try {
                    var listener: IEncoderInternal.IListener
                    var listenerExecutor: Executor
                    synchronized(listenerLock) {
                        listener = this@MediaCodecEncoder.listener
                        listenerExecutor = this@MediaCodecEncoder.listenerExecutor
                    }

                    val extractor = ClosableFrameExtractor(
                        codec, index, info, tag
                    )
                    listenerExecutor.execute {
                        try {
                            listener.onOutputFrame(extractor.frame)
                        } catch (t: Throwable) {
                            if (state.isRunning) {
                                handleError(t)
                            } else {
                                /**
                                 * Next streamer element could be stopped and we are still processing.
                                 * In that case, just log the error.
                                 */
                                Logger.w(
                                    tag,
                                    "OnOutputFrame error ${t.message} but codec is not running: $state."
                                )
                            }
                        } finally {
                            extractor.close()
                        }
                    }

                } catch (t: Throwable) {
                    handleError(t)
                }
            }

            else -> {
                try {
                    codec.releaseOutputBuffer(index, false)
                } catch (e: CodecException) {
                    Log.e(tag, "Failed to release output buffer", e)
                }
            }
        }

        if (info.hasEndOfStreamFlag) {
            reachEndOfStream()
        }
    }

    private inner class EncoderCallback : MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec, index: Int, info: BufferInfo
        ) {
            encoderExecutor.execute {
                processOutputFrameSync(codec, index, info)
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            encoderExecutor.execute {
                when {
                    !state.isRunning -> {
                        Logger.w(tag, "Receives input frame after codec is not running: $state.")
                        return@execute
                    }

                    input !is IEncoderInternal.IAsyncByteBufferInput -> {
                        Logger.w(tag, "Input buffer is only available for byte buffer input")
                        return@execute
                    }

                    else -> {
                        val frame = try {
                            val buffer = requireNotNull(mediaCodec.getInputBuffer(index))
                            input.listener.onFrameRequested(buffer)
                        } catch (t: Throwable) {
                            Logger.e(tag, "Failed to get input buffer: $t")
                            mediaCodec.queueInputBuffer(
                                index, 0, 0, 0, 0
                            )
                            return@execute
                        }

                        try {
                            queueInputFrame(index, frame)
                        } catch (t: Throwable) {
                            handleError(t)
                        }
                    }
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Logger.i(tag, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: CodecException) {
            handleError(e)
        }

        private fun queueInputFrame(
            index: Int, frame: Frame
        ) {
            mediaCodec.queueInputBuffer(
                index, frame.buffer.position(), frame.buffer.limit(), frame.pts /* in us */, 0
            )
        }
    }

    internal inner class SurfaceInput : IEncoderInternal.ISurfaceInput {
        private val obsoleteSurfaces = mutableListOf<Surface>()

        override var surface: Surface? = null
            private set

        override var listener = object : IEncoderInternal.ISurfaceInput.OnSurfaceUpdateListener {}
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

    internal inner class AsyncByteBufferInput : IEncoderInternal.IAsyncByteBufferInput {
        override lateinit var listener: IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener
    }

    internal inner class SyncByteBufferInput : IEncoderInternal.ISyncByteBufferInput {
        private fun queueInputFrameSync(frame: Frame) {
            val inputBufferId = mediaCodec.dequeueInputBuffer(0) // Don't block
            if (inputBufferId < 0) {
                if (inputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return
                }
                Logger.w(tag, "Failed to dequeue input buffer: $inputBufferId")
                return
            }
            val inputBuffer = requireNotNull(mediaCodec.getInputBuffer(inputBufferId))
            val size = min(frame.buffer.remaining(), inputBuffer.remaining())
            inputBuffer.put(frame.buffer, frame.buffer.position(), size)
            mediaCodec.queueInputBuffer(
                inputBufferId, 0, size, frame.pts, 0
            )
        }

        override fun queueInputFrame(frame: Frame) {
            encoderExecutor.execute {
                if (!state.isRunning) {
                    Logger.w(tag, "Receives input frame after codec is not running: $state.")
                    return@execute
                }

                try {
                    /**
                     * Queue input frame until the buffer is empty.
                     * The counter is used to avoid infinite loop in case of a buffer that is not
                     * fully consumed because of a really slow encoder
                     */
                    var counter = 0
                    while (frame.buffer.hasRemaining() && counter < 50) {
                        queueInputFrameSync(frame)
                        counter++

                        val bufferInfo = BufferInfo()
                        var outputBufferId: Int
                        while (mediaCodec.dequeueOutputBuffer(bufferInfo, 0) // Don't block
                                .also { outputBufferId = it } != MediaCodec.INFO_TRY_AGAIN_LATER
                        ) {
                            if (outputBufferId >= 0) {
                                processOutputFrameSync(mediaCodec, outputBufferId, bufferInfo)
                            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                Logger.i(tag, "Format changed: ${mediaCodec.outputFormat}")
                            }
                        }
                    }
                    if (frame.buffer.hasRemaining()) {
                        Logger.w(
                            tag,
                            "Failed to queue input frame: skipping: ${frame.buffer.remaining()} bytes"
                        )
                    }
                } catch (t: Throwable) {
                    handleError(t)
                }
            }
        }
    }

    private class ClosableFrameExtractor(
        private val codec: MediaCodec,
        private val index: Int,
        info: BufferInfo,
        private val tag: String
    ) : Closeable {

        val frame = Frame(codec, index, info)

        override fun close() {
            try {
                codec.releaseOutputBuffer(index, false)
            } catch (e: IllegalStateException) {
                Logger.w(tag, "Failed to release output buffer for code: ${e.message}")
            }
        }

        companion object {
            /**
             * Create a [Frame] from a [MediaCodec] output buffer
             *
             * @param mediaCodec the [MediaCodec] instance
             * @param index the buffer index
             * @param info the buffer info
             */
            private fun Frame(
                mediaCodec: MediaCodec, index: Int, info: BufferInfo
            ): Frame {
                val buffer = requireNotNull(mediaCodec.getOutputBuffer(index))
                return Frame(
                    buffer, info.presentationTimeUs, // pts
                    null, // dts
                    info.isKeyFrame, mediaCodec.outputFormat
                )
            }
        }
    }


    private enum class State {
        /**
         * The initial state.
         */
        IDLE,

        /**
         * The encoder is configured
         */
        CONFIGURED,

        STARTED,

        PAUSED,

        STOPPED,

        PENDING_START,

        PENDING_STOP,

        PENDING_RELEASE,

        ERROR,

        /** The state is when the encoder is released.  */
        RELEASED;

        val isRunning: Boolean
            get() = this == STARTED
    }

}
