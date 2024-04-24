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
package io.github.thibaultbee.streampack.streamers.bases

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.encoders.IEncoder
import io.github.thibaultbee.streampack.internal.encoders.IEncoderSettings
import io.github.thibaultbee.streampack.internal.encoders.mediacodec.AudioEncoderConfig
import io.github.thibaultbee.streampack.internal.encoders.mediacodec.MediaCodecEncoder
import io.github.thibaultbee.streampack.internal.encoders.mediacodec.VideoEncoderConfig
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.events.EventHandler
import io.github.thibaultbee.streampack.internal.gl.CodecSurface
import io.github.thibaultbee.streampack.internal.sources.IAudioSource
import io.github.thibaultbee.streampack.internal.sources.IAudioSourceSettings
import io.github.thibaultbee.streampack.internal.sources.IVideoSource
import io.github.thibaultbee.streampack.internal.sources.IVideoSourceSettings
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.helpers.IConfigurationHelper
import io.github.thibaultbee.streampack.streamers.helpers.StreamerConfigurationHelper
import io.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer


/**
 * Base class of all streamers.
 *
 * @param context the application context
 * @param internalVideoSource the video source
 * @param internalAudioSource the audio source
 * @param internalEndpoint the [IEndpoint] implementation
 * @param initialOnErrorListener initial [OnErrorListener]
 */
abstract class BaseStreamer(
    private val context: Context,
    protected val internalAudioSource: IAudioSource?,
    protected val internalVideoSource: IVideoSource?,
    protected val internalEndpoint: IEndpoint,
    initialOnErrorListener: OnErrorListener? = null
) : EventHandler(), IStreamer {
    /**
     * Listener that reports streamer error.
     * Supports only one listener.
     */
    override var onErrorListener: OnErrorListener? = initialOnErrorListener
    override val helper = StreamerConfigurationHelper(internalEndpoint.helper)

    private var isStreaming = false

    private var audioStreamId: Int? = null
    private var videoStreamId: Int? = null

    // Keep video configuration
    protected var videoConfig: VideoConfig? = null
    private var audioConfig: AudioConfig? = null

    private val sourceOrientationProvider = internalVideoSource?.orientationProvider

    // Only handle stream error (error on muxer, endpoint,...)
    /**
     * Internal usage only
     */
    final override val onInternalErrorListener = object : OnErrorListener {
        override fun onError(e: Exception) {
            onStreamError(e)
        }
    }

    private val audioEncoderListener = object : IEncoder.IListener {
        override fun onError(e: Exception) {
            onStreamError(e)
        }

        override fun onOutputFrame(frame: Frame) {
            audioStreamId?.let {
                try {
                    this@BaseStreamer.internalEndpoint.write(frame, it)
                } catch (e: Exception) {
                    throw StreamPackError(e)
                }
            }
        }
    }

    private val videoEncoderListener = object : IEncoder.IListener {
        override fun onError(e: Exception) {
            onStreamError(e)
        }

        override fun onOutputFrame(frame: Frame) {
            videoStreamId?.let {
                try {
                    frame.pts += internalVideoSource!!.timestampOffset
                    frame.dts = if (frame.dts != null) {
                        frame.dts!! + internalVideoSource.timestampOffset
                    } else {
                        null
                    }
                    this@BaseStreamer.internalEndpoint.write(frame, it)
                } catch (e: Exception) {
                    // Send exception to encoder
                    throw StreamPackError(e)
                }
            }
        }
    }

    /**
     * Manages error on stream.
     * Stops only stream.
     *
     * @param e triggered [Exception]
     */
    private fun onStreamError(e: Exception) {
        try {
            runBlocking {
                stopStream()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "onStreamError: Can't stop stream")
        } finally {
            onErrorListener?.onError(e)
        }
    }

    // SOURCES

    /**
     * The audio source.
     * It allows advanced audio settings.
     */
    override val audioSource: IAudioSourceSettings?
        get() = internalAudioSource

    /**
     * The video source.
     * It allows advanced video settings.
     */
    override val videoSource: IVideoSourceSettings?
        get() = internalVideoSource

    // ENCODERS

    private var internalAudioEncoder: MediaCodecEncoder? = null

    /**
     * The audio encoder.
     * Only valid when audio has been [configure]. It is null after [release].
     */
    override val audioEncoder: IEncoderSettings?
        get() = internalAudioEncoder

    private var internalVideoEncoder: MediaCodecEncoder? = null

    /**
     * The video encoder.
     * Only valid when audio has been [configure]. It is null after [release].
     */
    override val videoEncoder: IEncoderSettings?
        get() = internalVideoEncoder


    protected val codecSurface =
        if (internalVideoSource?.hasSurface == true) CodecSurface(sourceOrientationProvider) else null

    /**
     * Whether the streamer has audio.
     */
    val hasAudio = internalAudioSource != null

    /**
     * Whether the streamer has video.
     */
    val hasVideo = internalVideoSource != null

    /**
     * Configures audio settings.
     * It is the first method to call after a [BaseStreamer] instantiation.
     * It must be call when both stream and audio capture are not running.
     *
     * Use [IConfigurationHelper] to get value limits.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(audioConfig: AudioConfig) {
        require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
        require(internalAudioSource != null) { "Audio source must not be null" }

        this.audioConfig = audioConfig

        try {
            internalAudioSource.configure(audioConfig)

            internalAudioEncoder?.release()
            internalAudioEncoder =
                MediaCodecEncoder(
                    AudioEncoderConfig(audioConfig),
                    listener = audioEncoderListener
                ).apply {
                    if (input is MediaCodecEncoder.ByteBufferInput) {
                        input.listener =
                            object : IEncoder.IByteBufferInput.OnFrameRequestedListener {
                                override fun onFrameRequested(buffer: ByteBuffer): Frame {
                                    return internalAudioSource.getFrame(buffer)
                                }
                            }
                    } else {
                        throw UnsupportedOperationException("Audio encoder only support ByteBuffer mode")
                    }
                    configure()
                }
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    private fun buildVideoEncoder(
        videoConfig: VideoConfig,
        videoSource: IVideoSource
    ): MediaCodecEncoder {
        val videoEncoder = MediaCodecEncoder(
            VideoEncoderConfig(
                videoConfig,
                videoSource.hasSurface,
                videoSource.orientationProvider
            ), listener = videoEncoderListener
        )

        when (videoEncoder.input) {
            is MediaCodecEncoder.SurfaceInput -> {
                codecSurface!!.useHighBitDepth = videoConfig.isHdr
                videoEncoder.input.listener =
                    object : IEncoder.ISurfaceInput.OnSurfaceUpdateListener {
                        override fun onSurfaceUpdated(surface: Surface) {
                            Logger.d(TAG, "Updating with new encoder surface input")
                            codecSurface.outputSurface = surface
                        }
                    }
            }

            is MediaCodecEncoder.ByteBufferInput -> {
                videoEncoder.input.listener =
                    object : IEncoder.IByteBufferInput.OnFrameRequestedListener {
                        override fun onFrameRequested(buffer: ByteBuffer): Frame {
                            return videoSource.getFrame(buffer)
                        }
                    }
            }

            else -> {
                throw UnsupportedOperationException("Unknown input type")
            }
        }
        return videoEncoder
    }

    /**
     * Configures video settings.
     * It is the first method to call after a [BaseStreamer] instantiation.
     * It must be call when both stream and video capture are not running.
     *
     * Use [IConfigurationHelper] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     */
    override fun configure(videoConfig: VideoConfig) {
        require(hasVideo) { "Do not need to set video as it is a audio only streamer" }
        require(internalVideoSource != null) { "Video source must not be null" }

        this.videoConfig = videoConfig

        try {
            internalVideoSource.configure(videoConfig)

            internalVideoEncoder?.release()
            internalVideoEncoder = buildVideoEncoder(videoConfig, internalVideoSource).apply {
                configure()
            }
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    /**
     * Configures both video and audio settings.
     * It is the first method to call after a [BaseStreamer] instantiation.
     * It must be call when both stream and audio and video capture are not running.
     *
     * Use [IConfigurationHelper] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(audioConfig: AudioConfig, videoConfig: VideoConfig) {
        configure(audioConfig)
        configure(videoConfig)
    }

    /**
     * Starts audio/video stream.
     * Stream depends of the endpoint: Audio/video could be write to a file or send to a remote
     * device.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @see [stopStream]
     */
    override suspend fun startStream() {
        isStreaming = true
        try {
            val streams = mutableListOf<Config>()
            val orientedVideoConfig = if (hasVideo) {
                require(videoConfig != null) { "Requires video config" }
                /**
                 * If sourceOrientationProvider is not null, we need to get oriented size.
                 * For example, the [FlvMuxer] `onMetaData` event needs to know the oriented size.
                 */
                if (sourceOrientationProvider != null) {
                    val orientedSize =
                        sourceOrientationProvider.getOrientedSize(videoConfig!!.resolution)
                    videoConfig!!.copy(resolution = orientedSize)
                } else {
                    videoConfig!!
                }
            } else {
                null
            }
            if (orientedVideoConfig != null) {
                streams.add(orientedVideoConfig)
            }

            if (hasAudio) {
                require(audioConfig != null) { "Requires audio config" }
                streams.add(audioConfig!!)
            }

            val streamsIdMap = internalEndpoint.addStreams(streams)
            orientedVideoConfig?.let { videoStreamId = streamsIdMap[orientedVideoConfig] }
            audioConfig?.let { audioStreamId = streamsIdMap[audioConfig as Config] }

            internalEndpoint.startStream()

            internalAudioSource?.startStream()
            internalAudioEncoder?.startStream()

            internalVideoSource?.startStream()
            codecSurface?.startStream()
            internalVideoEncoder?.startStream()
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
    override suspend fun stopStream() {
        if (!isStreaming) {
            Logger.w(TAG, "Stream is not running")
            return
        }

        stopStreamImpl()

        internalAudioEncoder?.reset()
        internalVideoEncoder?.reset()
    }

    /**
     * Stops audio/video stream implementation.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamImpl() {
        internalVideoSource?.stopStream()
        codecSurface?.stopStream()
        internalVideoEncoder?.stopStream()
        internalAudioEncoder?.stopStream()
        internalAudioSource?.stopStream()

        internalEndpoint.stopStream()
    }

    /**
     * Releases recorders and encoders object.
     * It also stops preview if needed
     *
     * @see [configure]
     */
    override fun release() {
        internalAudioEncoder?.release()
        internalAudioEncoder = null
        codecSurface?.release()
        internalVideoEncoder?.release()
        internalVideoEncoder = null

        internalAudioSource?.release()
        internalVideoSource?.release()

        internalEndpoint.release()
    }

    companion object {
        private const val TAG = "BaseStreamer"
    }
}