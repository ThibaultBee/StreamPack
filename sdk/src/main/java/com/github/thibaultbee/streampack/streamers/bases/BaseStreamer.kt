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
package com.github.thibaultbee.streampack.streamers.bases

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.internal.data.Frame
import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.encoders.AudioMediaCodecEncoder
import com.github.thibaultbee.streampack.internal.encoders.IEncoderListener
import com.github.thibaultbee.streampack.internal.encoders.VideoMediaCodecEncoder
import com.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import com.github.thibaultbee.streampack.internal.events.EventHandler
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.sources.IFrameCapture
import com.github.thibaultbee.streampack.internal.sources.ISurfaceCapture
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import com.github.thibaultbee.streampack.utils.CameraStreamerConfigurationHelper
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

/**
 * Base class of all streamers.
 * Use this class, only if you want to implement a add new Audio or video source.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param videoCapture Video source
 * @param audioCapture Audio source
 * @param endpoint a [IEndpoint] implementation
 * @param logger a [ILogger] implementation
 */
abstract class BaseStreamer(
    private val context: Context,
    private val tsServiceInfo: ServiceInfo,
    protected val videoCapture: ISurfaceCapture<VideoConfig>?,
    protected val audioCapture: IFrameCapture<AudioConfig>?,
    protected val endpoint: IEndpoint,
    protected val logger: ILogger
) : EventHandler(), IStreamer {
    /**
     * Listener that reports streamer error.
     * Supports only one listener.
     */
    override var onErrorListener: OnErrorListener? = null

    private var audioTsStreamId: Short? = null
    private var videoTsStreamId: Short? = null

    // Keep video configuration
    protected var videoConfig: VideoConfig? = null
    private var audioConfig: AudioConfig? = null

    // Only handle stream error (error on muxer, endpoint,...)
    final override val onInternalErrorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            onStreamError(error)
        }
    }

    private val audioEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            return audioCapture!!.getFrame(buffer)
        }

        override fun onOutputFrame(frame: Frame) {
            audioTsStreamId?.let {
                try {
                    tsMux.encode(frame, it)
                } catch (e: Exception) {
                    throw StreamPackError(e)
                }
            }
        }
    }

    private val videoEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            // Not needed for video
            throw StreamPackError(RuntimeException("No video input on VideoEncoder"))
        }

        override fun onOutputFrame(frame: Frame) {
            videoTsStreamId?.let {
                try {
                    frame.pts += videoCapture!!.timestampOffset
                    frame.dts = if (frame.dts != null) {
                        frame.dts!! + videoCapture.timestampOffset
                    } else {
                        null
                    }
                    tsMux.encode(frame, it)
                } catch (e: Exception) {
                    // Send exception to encoder
                    throw StreamPackError(e)
                }
            }
        }
    }

    private val muxListener = object : IMuxerListener {
        override fun onOutputFrame(packet: Packet) {
            try {
                endpoint.write(packet)
            } catch (e: Exception) {
                // Send exception to encoder
                throw StreamPackError(e)
            }
        }
    }

    /**
     * Manages error on stream.
     * Stops only stream.
     *
     * @param error triggered [StreamPackError]
     */
    private fun onStreamError(error: StreamPackError) {
        try {
            stopStream()
            onErrorListener?.onError(error)
        } catch (e: Exception) {
            logger.e(this, "onStreamError: Can't stop stream")
        }
    }

    private var audioEncoder = if (audioCapture != null) {
        AudioMediaCodecEncoder(audioEncoderListener, onInternalErrorListener, logger)
    } else {
        null
    }
    protected var videoEncoder = if (videoCapture != null) {
        VideoMediaCodecEncoder(videoEncoderListener, onInternalErrorListener, context, logger)
    } else {
        null
    }

    protected var audioBitrate: Int
        get() = audioEncoder?.bitrate ?: throw UnsupportedOperationException("No audio source")
        set(value) {
            audioEncoder?.let { it.bitrate = value }
                ?: throw UnsupportedOperationException("No audio source")
        }
    protected var videoBitrate: Int
        get() = videoEncoder?.bitrate ?: throw UnsupportedOperationException("No video source")
        set(value) {
            videoEncoder?.let { it.bitrate = value }
                ?: throw UnsupportedOperationException("No video source")
        }

    private val tsMux = TSMuxer(muxListener)

    /**
     * Configures both video and audio settings.
     * It is the first method to call after a [BaseStreamer] instantiation.
     * It must be call when both stream and capture are not running.
     *
     * Use [CameraStreamerConfigurationHelper] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * Inside, it creates most of record and encoders object.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(audioConfig: AudioConfig?, videoConfig: VideoConfig?) {
        // Keep settings when we need to reconfigure
        this.videoConfig = videoConfig
        this.audioConfig = audioConfig

        try {
            audioConfig?.let {
                audioCapture?.configure(it)
                audioEncoder?.configure(it)
            }

            videoConfig?.let {
                videoCapture?.configure(it)
                videoEncoder?.configure(it)
            }

            endpoint.configure((videoConfig?.startBitrate ?: 0) + (audioConfig?.startBitrate ?: 0))
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    /**
     * Starts audio/video stream.
     * Stream depends of the endpoint: Audio/video could be write to a file or send to a remote
     * device.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @see [stopStream]
     */
    override fun startStream() {
        try {
            endpoint.startStream()

            val streams = mutableListOf<String>()
            videoEncoder?.mimeType?.let { streams.add(it) }
            audioEncoder?.mimeType?.let { streams.add(it) }

            tsMux.addService(tsServiceInfo)
            tsMux.addStreams(tsServiceInfo, streams)
            videoEncoder?.mimeType?.let { videoTsStreamId = tsMux.getStreams(it)[0].pid }
            audioEncoder?.mimeType?.let { audioTsStreamId = tsMux.getStreams(it)[0].pid }

            audioCapture?.startStream()
            audioEncoder?.startStream()

            videoCapture?.startStream()
            videoEncoder?.startStream()
        } catch (e: Exception) {
            stopStream()
            throw StreamPackError(e)
        }
    }

    /**
     * Stops audio/video stream.
     *
     * Internally, it resets audio and video recorders and encoders to get them ready for another
     * [startStream] session. It explains why preview could be restarted.
     *
     * @see [startStream]
     */
    override fun stopStream() {
        stopStreamImpl()

        // Encoder does not return to CONFIGURED state... so we have to reset everything for video...
        resetAudio()

        resetVideo()
    }

    /**
     * Stops audio/video stream implementation.
     *
     * @see [stopStream]
     */
    protected fun stopStreamImpl() {
        videoCapture?.stopStream()
        videoEncoder?.stopStream()
        audioEncoder?.stopStream()
        audioCapture?.stopStream()

        tsMux.stop()

        endpoint.stopStream()
    }

    /**
     * Prepares audio encoder for another session.
     *
     * @see [stopStream]
     */
    private fun resetAudio() {
        audioEncoder?.release()

        // Reconfigure
        audioConfig?.let {
            audioEncoder?.configure(it)
        }
    }

    /**
     * Reset video if needed
     *
     * @return true if children class wants to call [afterResetVideo]
     */
    protected abstract fun onResetVideo(): Boolean

    /**
     * Only calls if [onResetVideo] has returned [Boolean.true].
     */
    protected abstract suspend fun afterResetVideo()

    /**
     * Prepares video for another session.
     *
     * @see [stopStream]
     */
    private fun resetVideo() {
        val callAfterResetVideo = onResetVideo()
        videoEncoder?.release()

        // And restart...
        runBlocking {
            videoConfig?.let {
                videoEncoder?.configure(it)
            }
            videoCapture?.encoderSurface = videoEncoder?.inputSurface
            if (callAfterResetVideo) {
                afterResetVideo()
            }
        }
    }

    /**
     * Releases recorders and encoders object.
     * It also stops preview if needed
     *
     * @see [configure]
     */
    override fun release() {
        audioEncoder?.release()
        videoEncoder?.release()
        audioCapture?.release()
        videoCapture?.release()
        endpoint.release()
    }
}