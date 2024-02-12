/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.streamers

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.encoders.AudioMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.encoders.IEncoderListener
import io.github.thibaultbee.streampack.internal.encoders.MultiVideoMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.endpoints.FileWriter
import io.github.thibaultbee.streampack.internal.events.EventHandler
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.video.ExtendedVideoTag
import io.github.thibaultbee.streampack.internal.muxers.mp4.MP4Muxer
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider
import io.github.thibaultbee.streampack.internal.sources.AudioSource
import io.github.thibaultbee.streampack.internal.sources.camera.CameraSource
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.settings.BaseCameraStreamerSettings
import io.github.thibaultbee.streampack.streamers.settings.BaseStreamerAudioSettings
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID


class CameraDualLiveStreamer(
    private val context: Context,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : EventHandler() {

    private enum class EncoderIndex(val index: Int) {
        RTMP(0),
        FILE(1)
    }

    private val rtmpProducer =
        RtmpProducer().apply { onConnectionListener = initialOnConnectionListener }
    private val fileWriter = FileWriter()
    private var onErrorListener: OnErrorListener? = initialOnErrorListener

    private val audioSource = AudioSource()
    private val cameraSource = CameraSource(context)
    private val rtmpMuxer: IMuxer = FlvMuxer(writeToFile = false)
    private val fileMuxer: IMuxer = MP4Muxer()

    private var rtmpAudioStreamId: Int? = null
    private var rtmpVideoStreamId: Int? = null
    private var fileAudioStreamId: Int? = null
    private var fileVideoStreamId: Int? = null

    // Keep video configuration separate for rtmp and file
    private var rtmpVideoConfig: VideoConfig? = null
    private var fileVideoConfig: VideoConfig? = null
    // share the same audio config (and the same encoder)
    private var audioConfig: AudioConfig? = null

    private val sourceOrientationProvider: ISourceOrientationProvider
        get() = cameraSource.orientationProvider

    override val onInternalErrorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            onStreamError(error)
        }
    }

    private val audioEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            return audioSource.getFrame(buffer)
        }

        override fun onOutputFrame(frame: Frame) {
            val fileFrame = frame.clone()
            rtmpAudioStreamId?.let {
                try {
                    this@CameraDualLiveStreamer.rtmpMuxer.encode(frame, it)
                } catch (e: Exception) {
                    throw StreamPackError(e)
                }
            }
            fileAudioStreamId?.let {
                try{
                    this@CameraDualLiveStreamer.fileMuxer.encode(fileFrame,it)
                } catch(e:Exception){
                    throw StreamPackError(e)
                }
            }
        }
    }

    private class VideoEncoderListener(
        private val muxer: IMuxer,
        private val cameraSource: CameraSource,
    ) : IEncoderListener {

        private var streamId: Int? = null
        fun start(id: Int?) {
            streamId = id
        }

        override fun onInputFrame(buffer: ByteBuffer): Frame {
            return cameraSource.getFrame(buffer)
        }

        override fun onOutputFrame(frame: Frame) {
            streamId!!.let {
                try {
                    frame.pts += cameraSource.timestampOffset
                    frame.dts = if (frame.dts != null) {
                        frame.dts!! + cameraSource.timestampOffset
                    } else {
                        null
                    }
                    muxer.encode(frame, it)
                } catch (e: Exception) {
                    // Send exception to encoder
                    throw StreamPackError(e)
                }
            }
        }
    }

    private val rtmpVideoEncoderListener = VideoEncoderListener(rtmpMuxer, cameraSource)
    private val fileVideoEncoderListener = VideoEncoderListener(fileMuxer, cameraSource)

    private val rtmpMuxListener = object : IMuxerListener {
        override fun onOutputFrame(packet: Packet) {
            try {
                rtmpProducer.write(packet)
            } catch (e: Exception) {
                // Send exception to encoder
                throw StreamPackError(e)
            }
        }
    }
    private val fileMuxListener = object : IMuxerListener {
        override fun onOutputFrame(packet: Packet) {
            try {
                fileWriter.write(packet)
            } catch (e: Exception) {
                // Send exception to encoder
                throw StreamPackError(e)
            }
        }
    }

    private var audioEncoder =
        AudioMediaCodecEncoder(audioEncoderListener, onInternalErrorListener)
    private var videoEncoder = MultiVideoMediaCodecEncoder(
        listOf(rtmpVideoEncoderListener, fileVideoEncoderListener),
        onInternalErrorListener,
        sourceOrientationProvider
    )

    var onConnectionListener: OnConnectionListener? = initialOnConnectionListener
        set(value) {
            rtmpProducer.onConnectionListener = value
            field = value
        }

    var camera: String
        get() = cameraSource.cameraId
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            cameraSource.cameraId = value
        }

    var settings =
        BaseCameraStreamerSettings(
            cameraSource, BaseStreamerAudioSettings(audioSource, audioEncoder),
            videoEncoder.getTarget(EncoderIndex.RTMP.index)
        )

    init {
        rtmpMuxer.sourceOrientationProvider = sourceOrientationProvider
        rtmpMuxer.listener = rtmpMuxListener
        fileMuxer.sourceOrientationProvider = sourceOrientationProvider
        fileMuxer.listener = fileMuxListener
    }

    private fun onStreamError(error: StreamPackError) {
        try {
            runBlocking {
                stopStream()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "onStreamError: Can't stop stream")
        } finally {
            onErrorListener?.onError(error)
        }
    }

    suspend fun startStream() {
        try {
            rtmpProducer.startStream()

            val rtmpStreams = mutableListOf<Config>()
            require(rtmpVideoConfig != null) { "Requires video config" }
            rtmpStreams.add(rtmpVideoConfig!!)
            require(audioConfig != null) { "Requires audio config" }
            rtmpStreams.add(audioConfig!!)

            val fileStreams = mutableListOf<Config>()
            require(fileVideoConfig != null) { "Requires video config" }
            fileStreams.add(fileVideoConfig!!)
            require(audioConfig != null) { "Requires audio config" }
            fileStreams.add(audioConfig!!)


            val rtmpStreamsIdMap = rtmpMuxer.addStreams(rtmpStreams)
            rtmpVideoConfig?.let { rtmpVideoStreamId = rtmpStreamsIdMap[rtmpVideoConfig as Config] }
            audioConfig?.let { rtmpAudioStreamId = rtmpStreamsIdMap[audioConfig as Config] }

            val fileStreamsIdMap = fileMuxer.addStreams(fileStreams)
            fileVideoConfig?.let { fileVideoStreamId = fileStreamsIdMap[fileVideoConfig as Config] }
            audioConfig?.let { fileAudioStreamId = fileStreamsIdMap[audioConfig as Config] }

            rtmpVideoEncoderListener.start(rtmpVideoStreamId)
            fileVideoEncoderListener.start(fileVideoStreamId)

            rtmpMuxer.startStream()
            fileMuxer.startStream()

            audioSource.startStream()
            audioEncoder.startStream()

            cameraSource.encoderSurface = videoEncoder.inputSurface
            cameraSource.startStream()
            videoEncoder.startStream(EncoderIndex.RTMP.index)
            videoEncoder.startStream(EncoderIndex.FILE.index)
        } catch (e: Exception) {
            stopStream()
            throw StreamPackError(e)
        }
    }

    suspend fun stopStream() {
        try {
            cameraSource.stopStream()
            videoEncoder.stopStream(EncoderIndex.RTMP.index)
            videoEncoder.stopStream(EncoderIndex.FILE.index)
            audioEncoder.stopStream()
            audioSource.stopStream()

            rtmpMuxer.stopStream()
            fileMuxer.stopStream()

            rtmpProducer.stopStream()
            fileWriter.stopStream()

            // RECORDIMPL : do we really need to totally recreate all these
            // things? I wonder since we have fixed the stopStream bug on
            // mediacodec. We should try
            //

            // Encoder does not return to CONFIGURED state... so we have to reset everything...
            audioEncoder.release()

            // Reconfigure
            audioConfig?.let {
                audioEncoder.configure(it)
            }
            videoEncoder.releaseTargets()

            // And restart...
            rtmpVideoConfig?.let {
                videoEncoder.configure(EncoderIndex.RTMP.index, it)
            }
            fileVideoConfig?.let {
                videoEncoder.configure(EncoderIndex.FILE.index, it)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "stopStream-exception:${e.message}")
        }
    }

    suspend fun connect(url: String) {
        require(rtmpVideoConfig != null) {
            "Video config must be set before connecting to send the video codec in the connect message"
        }
        val codecMimeType = rtmpVideoConfig!!.mimeType
        if (ExtendedVideoTag.isSupportedCodec(codecMimeType)) {
            rtmpProducer.supportedVideoCodecs = listOf(codecMimeType)
        }
        rtmpProducer.supportedVideoCodecs = listOf(rtmpVideoConfig!!.mimeType)
        rtmpProducer.connect(url)

        val filename = "${UUID.randomUUID()}.mp4"
        val file = File(context.filesDir, filename)
        if (!file.exists()) {
            file.createNewFile()
        }
        fileWriter.file = file
    }

    fun disconnect() {
        rtmpProducer.disconnect()
    }

    val isConnected: Boolean
        get() = rtmpProducer.isConnected

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configureAudio(audioConfig: AudioConfig) {
        // Keep settings when we need to reconfigure
        this.audioConfig = audioConfig

        try {
            audioSource.configure(audioConfig)
            audioEncoder.release()
            audioEncoder.configure(audioConfig)

            rtmpProducer.configure((rtmpVideoConfig?.startBitrate ?: 0) + audioConfig.startBitrate)
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    fun configureVideo(videoConfig: VideoConfig) {
        // Keep settings when we need to reconfigure
        this.rtmpVideoConfig = videoConfig
        this.fileVideoConfig = videoConfig

        try {
            cameraSource.configure(videoConfig)
            videoEncoder.releaseTargets()
            videoEncoder.configure(EncoderIndex.RTMP.index, videoConfig)
            videoEncoder.configure(EncoderIndex.FILE.index, videoConfig)

            rtmpProducer.configure(videoConfig.startBitrate + (audioConfig?.startBitrate ?: 0))
            fileWriter.configure(0)
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface, cameraId: String) {
        require(rtmpVideoConfig != null) { "Video has not been configured!" }
        runBlocking {
            try {
                cameraSource.previewSurface = previewSurface
                cameraSource.encoderSurface = videoEncoder.inputSurface
                cameraSource.startPreview(cameraId)
            } catch (e: Exception) {
                stopPreview()
                throw StreamPackError(e)
            }
        }
    }

    fun stopPreview() {
        runBlocking {
            stopStream()
        }
        cameraSource.stopPreview()
    }

    fun release() {
        stopPreview()
        audioEncoder.release()
        videoEncoder.releaseTargets()
        videoEncoder.releaseInput()
        audioSource.release()
        cameraSource.release()

        rtmpMuxer.release()
        fileMuxer.release()

        rtmpProducer.release()
        fileWriter.release()
    }

    companion object {
        private const val TAG = "BaseStreamer"
    }
}
