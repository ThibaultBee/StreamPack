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
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.EncoderMode
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.hasEndOfStreamFlag
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.isKeyFrame
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.extensions.isValid
import io.github.thibaultbee.streampack.core.elements.utils.extensions.extra
import io.github.thibaultbee.streampack.core.elements.utils.extensions.put
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * The [MediaCodec] encoder implementation.
 *
 * @param encoderConfig the encoder configuration
 */
internal class MediaCodecEncoder
internal constructor(
    private val encoderConfig: EncoderConfig<*>,
    private val listener: IEncoderInternal.IListener,
    private val defaultDispatcher: CoroutineDispatcher,
    private val processDispatcher: CoroutineDispatcher
) : IEncoderInternal {
    private val coroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val mutex = Mutex()

    private val mediaCodec: MediaCodec
    private val format: MediaFormat
    private var outputFormat: MediaFormat? = null
    private val frameFactory by lazy { FrameFactory(mediaCodec, isVideo) }

    private val isVideo = encoderConfig.isVideo
    private val tag = if (isVideo) VIDEO_ENCODER_TAG else AUDIO_ENCODER_TAG + "(${this.hashCode()})"


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

    private fun configureUnsafe() {
        try {
            if (input !is SyncByteBufferInput) {
                /**
                 * Set encoder callback without handler.
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
            releaseUnsafe()
            throw t
        }
    }

    override suspend fun configure() {
        withMutexContext {
            configureUnsafe()
        }
    }

    private fun resetUnsafe() {
        if (state == State.CONFIGURED) {
            return
        }

        try {
            mediaCodec.reset()
        } catch (_: IllegalStateException) {
            Logger.d(tag, "Failed to reset")
        } finally {
            configureUnsafe()
        }
    }

    override suspend fun reset() {
        withMutexContext {
            resetUnsafe()
        }
    }

    private fun startStreamUnsafe() {
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
        withMutexContext {
            startStreamUnsafe()
        }
    }

    private fun stopStreamUnsafe() {
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
                } catch (_: IllegalStateException) {
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
        withMutexContext {
            stopStreamUnsafe()
        }
    }

    private fun releaseUnsafe() {
        try {
            mediaCodec.stop()
        } catch (_: IllegalStateException) {
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

    override suspend fun release() {
        withMutexContext {
            releaseUnsafe()
        }
        coroutineScope.cancel()
    }

    private fun notifyError(t: Throwable) {
        coroutineScope.launch {
            listener.onError(t)
        }
    }

    private suspend fun handleErrorUnsafe(t: Throwable) {
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
                    stopStreamUnsafe()
                } catch (e: Throwable) {
                    Logger.w(tag, "Failed to stop stream", e)
                }
                setState(State.ERROR)
                notifyError(t)
            }
        }
    }

    private suspend fun handleError(t: Throwable) {
        withMutexContext {
            handleErrorUnsafe(t)
        }
    }

    private fun reachEndOfStream() {
        Logger.i(tag, "End of stream reached")
        coroutineScope.launch {
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

    private suspend fun withMutexContext(block: suspend () -> Unit) {
        withContext(defaultDispatcher) {
            mutex.withLock {
                block()
            }
        }
    }

    private fun launchProcessingMutex(block: suspend () -> Unit) {
        coroutineScope.launch(processDispatcher) {
            mutex.withLock {
                block()
            }
        }
    }

    private suspend fun processOutputFrameUnsafe(codec: MediaCodec, index: Int, info: BufferInfo) {
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
                    val frame = frameFactory.frame(
                        index, outputFormat!!, info, tag
                    )
                    try {
                        listener.outputChannel.send(frame)
                    } catch (t: Throwable) {
                        if (state.isRunning) {
                            handleErrorUnsafe(t)
                        } else {
                            /**
                             * Next streamer element could be stopped and we are still processing.
                             * In that case, just log the error.
                             */
                            Logger.w(
                                tag,
                                "Sending frame to channel failed: ${t.message} but codec is not running: $state."
                            )
                        }
                    }
                } catch (t: Throwable) {
                    handleErrorUnsafe(t)
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
            launchProcessingMutex {
                processOutputFrameUnsafe(codec, index, info)
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            launchProcessingMutex {
                when {
                    !state.isRunning -> {
                        Logger.w(tag, "Receives input frame after codec is not running: $state.")
                        return@launchProcessingMutex
                    }

                    input !is IEncoderInternal.IAsyncByteBufferInput -> {
                        Logger.w(tag, "Input buffer is only available for byte buffer input")
                        return@launchProcessingMutex
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
                            return@launchProcessingMutex
                        }

                        try {
                            queueInputFrame(index, frame)
                        } catch (t: Throwable) {
                            handleErrorUnsafe(t)
                        } finally {
                            frame.close()
                        }
                    }
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            outputFormat = format
            Logger.i(tag, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: CodecException) {
            coroutineScope.launch {
                handleError(e)
            }
        }

        private fun queueInputFrame(
            index: Int, frame: RawFrame
        ) {
            mediaCodec.queueInputBuffer(
                index,
                frame.rawBuffer.position(),
                frame.rawBuffer.limit(),
                frame.timestampInUs /* in us */,
                0
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
        private val bufferInfo = BufferInfo()

        /**
         * Process output frame synchronously
         *
         * @param frame the frame to process
         * @return [Boolean.true] if the frame is processed, [Boolean.false] otherwise
         */
        private fun queueInputFrameSync(frame: RawFrame): Boolean {
            val inputBufferId = mediaCodec.dequeueInputBuffer(0) // Don't block
            if (inputBufferId < 0) {
                if (inputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return false
                }
                Logger.w(tag, "Failed to dequeue input buffer: $inputBufferId")
                return false
            }
            val inputBuffer = requireNotNull(mediaCodec.getInputBuffer(inputBufferId))
            val size = min(frame.rawBuffer.remaining(), inputBuffer.remaining())
            inputBuffer.put(frame.rawBuffer, frame.rawBuffer.position(), size)
            mediaCodec.queueInputBuffer(
                inputBufferId, 0, size, frame.timestampInUs, 0
            )
            return true
        }

        override fun queueInputFrame(frame: RawFrame) {
            launchProcessingMutex {
                if (!state.isRunning) {
                    Logger.w(tag, "Receives input frame after codec is not running: $state.")
                    return@launchProcessingMutex
                }

                try {
                    /**
                     * Queue input frame until the buffer is empty.
                     * The counter is used to avoid infinite loop in case of a buffer that is not
                     * fully consumed because of a really slow encoder
                     */
                    var counter = 0
                    while (frame.rawBuffer.hasRemaining() && counter < 10) {
                        if (queueInputFrameSync(frame)) {
                            counter = 0
                        } else {
                            counter++
                        }

                        var outputBufferId: Int
                        while (mediaCodec.dequeueOutputBuffer(bufferInfo, 0) // Don't block
                                .also { outputBufferId = it } != MediaCodec.INFO_TRY_AGAIN_LATER
                        ) {
                            if (outputBufferId >= 0) {
                                processOutputFrameUnsafe(mediaCodec, outputBufferId, bufferInfo)
                            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                outputFormat = mediaCodec.outputFormat
                                Logger.i(tag, "Format changed: ${mediaCodec.outputFormat}")
                            }
                        }
                    }
                    if (frame.rawBuffer.hasRemaining()) {
                        Logger.w(
                            tag,
                            "Failed to queue input frame: skipping: ${frame.rawBuffer.remaining()} bytes"
                        )
                    }
                } catch (t: Throwable) {
                    handleErrorUnsafe(t)
                } finally {
                    // Release frame resources
                    frame.close()
                }
            }
        }
    }

    /**
     * A workaround to address the fact that some AAC encoders do not provide frame with `presentationTimeUs` in order.
     * If a frame is received with a timestamp lower or equal to the previous one, it is corrected by adding 1 to the previous timestamp.
     *
     * @param codec the [MediaCodec] instance
     * @param isVideo true if the codec is a video codec, false otherwise
     */
    class FrameFactory(
        private val codec: MediaCodec,
        private val isVideo: Boolean
    ) {
        private var previousPresentationTimeUs = 0L

        /**
         * Create a [Frame] from a [RawFrame]
         *
         * @return the created frame
         */
        fun frame(
            index: Int, outputFormat: MediaFormat, info: BufferInfo, tag: String
        ): FrameWithCloseable {
            var pts = info.presentationTimeUs
            if (pts <= previousPresentationTimeUs) {
                pts = previousPresentationTimeUs + 1
                Logger.w(tag, "Correcting timestamp: $pts <= $previousPresentationTimeUs")
            }
            previousPresentationTimeUs = pts
            return createFrame(codec, index, outputFormat, pts, info.isKeyFrame, tag)
        }

        /**
         * Create a [Frame] from a [MediaCodec] output buffer
         *
         * @param codec the [MediaCodec] instance
         * @param index the buffer index
         * @param info the buffer info
         */
        private fun createFrame(
            codec: MediaCodec,
            index: Int,
            outputFormat: MediaFormat,
            ptsInUs: Long,
            isKeyFrame: Boolean,
            tag: String
        ): FrameWithCloseable {
            val buffer = requireNotNull(codec.getOutputBuffer(index))
            return FrameWithCloseable(
                buffer,
                ptsInUs, // pts
                null, // dts
                isKeyFrame,
                try {
                    if (isKeyFrame || !isVideo) {
                        outputFormat.extra
                    } else {
                        null
                    }
                } catch (_: Throwable) {
                    null
                },
                outputFormat,
                onClosed = {
                    try {
                        codec.releaseOutputBuffer(index, false)
                    } catch (t: Throwable) {
                        Logger.w(tag, "Failed to release output buffer for code: ${t.message}")
                    }
                })
        }

    }

    companion object {
        private const val AUDIO_ENCODER_TAG = "AudioEncoder"
        private const val VIDEO_ENCODER_TAG = "VideoEncoder"
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
