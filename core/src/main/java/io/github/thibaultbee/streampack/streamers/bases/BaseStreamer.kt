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
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.encoders.AudioMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.encoders.IEncoderListener
import io.github.thibaultbee.streampack.internal.encoders.VideoMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.events.EventHandler
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.sources.IAudioCapture
import io.github.thibaultbee.streampack.internal.sources.IVideoCapture
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.helpers.IConfigurationHelper
import io.github.thibaultbee.streampack.streamers.helpers.StreamerConfigurationHelper
import io.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import io.github.thibaultbee.streampack.streamers.settings.BaseStreamerSettings
import java.nio.ByteBuffer


/**
 * Base class of all streamers.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param videoCapture Video source
 * @param audioCapture Audio source
 * @param manageVideoOrientation Set to [Boolean.true] to rotate video according to device orientation.
 * @param muxer a [IMuxer] implementation
 * @param endpoint a [IEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
abstract class BaseStreamer(
    private val context: Context,
    protected val logger: ILogger = StreamPackLogger(),
    protected val audioCapture: IAudioCapture?,
    protected val videoCapture: IVideoCapture?,
    manageVideoOrientation: Boolean,
    private val muxer: IMuxer,
    protected val endpoint: IEndpoint,
    initialOnErrorListener: OnErrorListener? = null
) : EventHandler(), IStreamer {
    /**
     * Listener that reports streamer error.
     * Supports only one listener.
     */
    override var onErrorListener: OnErrorListener? = initialOnErrorListener
    override val helper = StreamerConfigurationHelper(muxer.helper)

    private var audioTsStreamId: Int? = null
    private var videoTsStreamId: Int? = null

    // Keep video configuration
    protected var videoConfig: VideoConfig? = null
    private var audioConfig: AudioConfig? = null

    // Only handle stream error (error on muxer, endpoint,...)
    /**
     * Internal usage only
     */
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
                    this@BaseStreamer.muxer.encode(frame, it)
                } catch (e: Exception) {
                    throw StreamPackError(e)
                }
            }
        }
    }

    private val videoEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            return videoCapture!!.getFrame(buffer)
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
                    this@BaseStreamer.muxer.encode(frame, it)
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

    protected var audioEncoder = if (audioCapture != null) {
        AudioMediaCodecEncoder(audioEncoderListener, onInternalErrorListener, logger)
    } else {
        null
    }
    protected var videoEncoder = if (videoCapture != null) {
        VideoMediaCodecEncoder(
            videoEncoderListener,
            onInternalErrorListener,
            context,
            videoCapture.hasSurface,
            manageVideoOrientation,
            logger
        )
    } else {
        null
    }
    override val settings = BaseStreamerSettings(audioCapture, audioEncoder, videoEncoder)

    private val hasAudio: Boolean
        get() = audioCapture != null
    private val hasVideo: Boolean
        get() = videoCapture != null

    init {
        muxer.manageVideoOrientation = manageVideoOrientation
        muxer.listener = muxListener
    }

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

        // Keep settings when we need to reconfigure
        this.audioConfig = audioConfig

        try {
            audioCapture?.configure(audioConfig)
            audioEncoder?.configure(audioConfig)

            endpoint.configure((videoConfig?.startBitrate ?: 0) + audioConfig.startBitrate)
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
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

        // Keep settings when we need to reconfigure
        this.videoConfig = videoConfig

        try {
            videoCapture?.configure(videoConfig)
            videoEncoder?.configure(videoConfig)

            endpoint.configure(videoConfig.startBitrate + (audioConfig?.startBitrate ?: 0))
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
    override fun startStream() {
        try {
            endpoint.startStream()

            val streams = mutableListOf<Config>()
            if (hasVideo) {
                require(videoConfig != null) { "Requires video config" }
                streams.add(videoConfig!!)
            }
            if (hasAudio) {
                require(audioConfig != null) { "Requires video config" }
                streams.add(audioConfig!!)
            }

            val streamsIdMap = this.muxer.addStreams(streams)
            videoConfig?.let { videoTsStreamId = streamsIdMap[videoConfig as Config] }
            audioConfig?.let { audioTsStreamId = streamsIdMap[audioConfig as Config] }

            muxer.startStream()

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

        // Encoder does not return to CONFIGURED state... so we have to reset everything...
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

        muxer.stopStream()

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
     * Prepares video for another session.
     *
     * @see [stopStream]
     */
    private fun resetVideo() {
        videoEncoder?.release()

        // And restart...
        videoConfig?.let {
            videoEncoder?.configure(it)
        }
        videoCapture?.encoderSurface = videoEncoder?.inputSurface
    }

    /**
     * Releases recorders and encoders object.
     * It also stops preview if needed
     *
     * @see [configure]
     */
    override fun release() {
        audioEncoder?.release()
        videoEncoder?.codecSurface?.dispose()
        videoEncoder?.release()
        audioCapture?.release()
        videoCapture?.release()

        muxer.release()

        endpoint.release()
    }
}