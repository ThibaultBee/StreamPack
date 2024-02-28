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
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
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
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
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

interface RtmpStatsListener {
    fun onInsufficientBandwidth(currentBytesOutPerSecond: Long, currentQueueBytesOut: Long)
    fun onSufficientBandwidth(currentBytesOutPerSecond: Long, currentQueueBytesOut: Long)
    fun updateStats(currentBytesOutPerSecond: Long, currentQueueBytesOut: Long)
}

interface RecordingListener {
    fun onRecordingStarted()
    fun onRecordingError(errorMessage: String)
    fun onRecordingFinished(url: String)
}

class CameraDualLiveStreamer(
    private val context: Context,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null,
    private val rtmpStatsListener: RtmpStatsListener,
    private val recordingListener: RecordingListener,
) : EventHandler() {

    private enum class EncoderIndex(val index: Int) {
        RTMP(0),
        FILE(1)
    }

    private val fileErrorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            recordingListener.onRecordingError(error.message ?: "$error")
        }
    }

    private val rtmpProducer =
        RtmpProducer().apply { onConnectionListener = initialOnConnectionListener }
    private val fileWriter = FileWriter(fileErrorListener)
    private var onErrorListener: OnErrorListener? = initialOnErrorListener

    private val audioSource = AudioSource()
    private val cameraSource = CameraSource(context)

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

    private class MuxerStreams(
        cameraSource: CameraSource,
        val muxer: IMuxer,
        sourceOrientationProvider: ISourceOrientationProvider,
        muxListener: IMuxerListener,
        var audioStreamId: Int? = null,
        var videoStreamId: Int? = null,
        val videoEncoderListener: VideoEncoderListener = VideoEncoderListener(muxer, cameraSource)
    ) {
        init {
            muxer.sourceOrientationProvider = sourceOrientationProvider
            muxer.listener = muxListener
        }

        var started: Boolean = false
            private set

        fun setupStreams(audioConfig: Config, videoConfig: Config) {
            val streams = mutableListOf<Config>()
            require(videoConfig != null) { "Requires video config" }
            streams.add(videoConfig!!)
            require(audioConfig != null) { "Requires audio config" }
            streams.add(audioConfig!!)

            val streamsIdMap = muxer.addStreams(streams)
            videoStreamId = streamsIdMap[videoConfig]
            audioStreamId = streamsIdMap[audioConfig]
        }

        fun start() {
            videoEncoderListener.start(videoStreamId)
            muxer.startStream()
            started = true
        }

        fun stop() {
            muxer.stopStream()
            started = false
        }

        fun release() {
            muxer.release()
        }
    }

    private val rtmpMuxerStreams = MuxerStreams(
        cameraSource,
        FlvMuxer(writeToFile = false),
        sourceOrientationProvider,
        rtmpMuxListener
    )
    private val fileMuxerStreams = MuxerStreams(
        cameraSource,
        MP4Muxer(),
        sourceOrientationProvider,
        fileMuxListener
    )

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
            try {
                val frames = mutableListOf(frame)
                if (rtmpMuxerStreams.started && fileMuxerStreams.started) {
                    frames.add(frame.clone())
                }
                if (rtmpMuxerStreams.started) {
                    val f = frames.removeAt(0)
                    rtmpMuxerStreams.audioStreamId?.let {
                        this@CameraDualLiveStreamer.rtmpMuxerStreams.muxer.encode(f, it)
                    }
                }
                if (fileMuxerStreams.started) {
                    val f = frames.removeAt(0)
                    fileMuxerStreams.audioStreamId?.let {
                        this@CameraDualLiveStreamer.fileMuxerStreams.muxer.encode(f, it)
                    }
                }
            } catch (e: Exception) {
                throw StreamPackError(e)
            }
        }
    }

    private var audioEncoder =
        AudioMediaCodecEncoder(audioEncoderListener, onInternalErrorListener)
    private var multiEncoder = MultiVideoMediaCodecEncoder(
        listOf(rtmpMuxerStreams.videoEncoderListener, fileMuxerStreams.videoEncoderListener),
        onInternalErrorListener,
        sourceOrientationProvider
    )

    var camera: String
        get() = cameraSource.cameraId
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            cameraSource.cameraId = value
        }

    var settings =
        BaseCameraStreamerSettings(
            cameraSource, BaseStreamerAudioSettings(audioSource, audioEncoder),
            multiEncoder.getTarget(EncoderIndex.RTMP.index)
        )

    private fun onStreamError(error: StreamPackError) {
        try {
            runBlocking {
                stopRtmp()
                stopRecording()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "onStreamError: Can't stop stream")
        } finally {
            onErrorListener?.onError(error)
        }
    }

    private fun startSources() {
        // does this need to go before we start all the streams?
        audioConfig?.let {
            audioEncoder.configure(it)
        }
        audioSource.startStream()
        audioEncoder.startStream()

        cameraSource.encoderSurface = multiEncoder.inputSurface
        cameraSource.startStream()
    }

    private fun stopSources() {
        cameraSource.stopStream()
        audioSource.stopStream()
        audioEncoder.stopStream()
        audioEncoder.release()
    }

    private suspend fun start(
        muxerStreams: MuxerStreams,
        videoConfig: VideoConfig?,
        encoderIndex: Int,
        endpoint: IEndpoint
    ) {
        if (muxerStreams.started) {
            return
        }
        val shouldStartSources = !fileMuxerStreams.started && !rtmpMuxerStreams.started
        try {
            require(!multiEncoder.getTarget(encoderIndex).isActive) { "muxer streams and encoder target should be in sync" }
            require(videoConfig != null) { "rtmp video config required" }
            endpoint.startStream()
            multiEncoder.configure(encoderIndex, videoConfig!!)
            muxerStreams.setupStreams(audioConfig!!, videoConfig!!)
            muxerStreams.start()

            if (shouldStartSources) {
                startSources()
            }
            multiEncoder.startStream(encoderIndex)
        } catch (e: Exception) {
            Logger.e(TAG, "startStream($encoderIndex)-exception:${e.message}")
            throw StreamPackError(e)
        }
    }

    suspend fun startRtmp() {
        start(rtmpMuxerStreams, rtmpVideoConfig, EncoderIndex.RTMP.index, rtmpProducer)
    }

    suspend fun startRecording() {
        if (fileMuxerStreams.started) {
            return
        }
        val filename = "${UUID.randomUUID()}.mp4"
        val file = File(context.filesDir, filename)
        if (!file.exists()) {
            file.createNewFile()
        }
        fileWriter.file = file

        start(fileMuxerStreams, fileVideoConfig, EncoderIndex.FILE.index, fileWriter)
        recordingListener.onRecordingStarted()
    }

    private suspend fun stop(
        muxerStreams: MuxerStreams,
        encoderIndex: Int,
        endpoint: IEndpoint
    ):Boolean {
        if (!muxerStreams.started) {
            return false
        }
        try {
            multiEncoder.stopStream(encoderIndex)
            muxerStreams.stop()
            endpoint.stopStream()
            multiEncoder.releaseTarget(encoderIndex)
            val shouldStopSources = !fileMuxerStreams.started && !rtmpMuxerStreams.started
            if (shouldStopSources) {
                stopSources()
            }
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "stopStream($encoderIndex)-exception:${e.message}")
            throw StreamPackError(e)
        }
    }

    suspend fun stopRtmp() {
        stop(rtmpMuxerStreams, EncoderIndex.RTMP.index, rtmpProducer)
    }

    suspend fun stopRecording() {
        if(stop(fileMuxerStreams, EncoderIndex.FILE.index, fileWriter)) {
            recordingListener.onRecordingFinished("file://${fileWriter.file?.absolutePath}")
        }
    }

    suspend fun connect(url: String) {
        connectionId++
        require(rtmpVideoConfig != null) {
            "Video config must be set before connecting to send the video codec in the connect message"
        }
        val codecMimeType = rtmpVideoConfig!!.mimeType
        if (ExtendedVideoTag.isSupportedCodec(codecMimeType)) {
            rtmpProducer.supportedVideoCodecs = listOf(codecMimeType)
        }
        rtmpProducer.supportedVideoCodecs = listOf(rtmpVideoConfig!!.mimeType)
        rtmpProducer.connect(url)
        startMonitoring()
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

    fun configureStreamingVideo(videoConfig: VideoConfig) {
        // don't allow configuring while active
        if (this.multiEncoder.getTarget(EncoderIndex.RTMP.index).isActive) {
            throw StreamPackError("Cannot configure stream while active.")
        }
        // Keep settings when we need to reconfigure
        this.rtmpVideoConfig = videoConfig

        try {
            // in general, streaming and recording are analogous. But
            // cameraSource configures fps and dynamicrange profile
            cameraSource.configure(videoConfig)
            multiEncoder.configure(EncoderIndex.RTMP.index, videoConfig)
            rtmpProducer.configure(videoConfig.startBitrate + (audioConfig?.startBitrate ?: 0))
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    fun configureRecordingVideo(videoConfig: VideoConfig) {
        // don't allow configuring while active
        if (this.multiEncoder.getTarget(EncoderIndex.FILE.index).isActive) {
            throw StreamPackError("Cannot configure stream while active.")
        }
        this.fileVideoConfig = videoConfig

        try {
            multiEncoder.configure(EncoderIndex.FILE.index, videoConfig)
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
                cameraSource.encoderSurface = multiEncoder.inputSurface
                cameraSource.startPreview(cameraId)
            } catch (e: Exception) {
                stopPreview()
                throw StreamPackError(e)
            }
        }
    }

    fun stopPreview() {
        runBlocking {
            stopRecording()
            stopRtmp()
        }
        cameraSource.stopPreview()
    }

    fun release() {
        stopPreview()
        audioEncoder.release()
        multiEncoder.releaseTarget(EncoderIndex.RTMP.index)
        multiEncoder.releaseTarget(EncoderIndex.FILE.index)
        multiEncoder.releaseInput()
        audioSource.release()
        cameraSource.release()

        rtmpMuxerStreams.release()
        fileMuxerStreams.release()

        rtmpProducer.release()
        fileWriter.release()
    }

    private val uid = context.applicationContext.applicationInfo.uid
    private var previousTxBytes = TrafficStats.getUidTxBytes(uid)
    private var previousGenBytes: Long = 0
    private val txBytesOverheadSlope = 0.62
    private val txBytesOverheadIntercept = 1000

    private val measureInterval = 3
    private val previousQueueBytesOut: MutableList<Long> = mutableListOf()
    private val txBytesMovingAvg: MutableList<Long> = mutableListOf()

    private var connectionId = 0; // monotonically increasing
    private fun startMonitoring() {
        var monitorConnectionId = connectionId
        txBytesMovingAvg.clear()
        previousQueueBytesOut.clear()
        previousTxBytes = TrafficStats.getUidTxBytes(uid)
        previousGenBytes = rtmpProducer.bytesSent.toLong()

        val handler = Handler(Looper.getMainLooper())
        val dataUsageChecker = object : Runnable {
            override fun run() {
                val currentTxBytes = TrafficStats.getUidTxBytes(uid)
                val currentGenBytes = rtmpProducer.bytesSent.toLong()

                val deltaTxBytes = currentTxBytes - previousTxBytes
                val deltaGenBytes = currentGenBytes - previousGenBytes
                val estimatedRealDeltaTxBytes =
                    (deltaTxBytes * txBytesOverheadSlope + txBytesOverheadIntercept).toLong()

                val newQueueBytesOut = deltaGenBytes - estimatedRealDeltaTxBytes
                val currentQueueBytesOut =
                    maxOf(newQueueBytesOut + (previousQueueBytesOut.lastOrNull() ?: 0), 0)
                previousQueueBytesOut.add(currentQueueBytesOut)
                txBytesMovingAvg.add(estimatedRealDeltaTxBytes)

                // Prepare for the next check
                previousTxBytes = currentTxBytes
                previousGenBytes = currentGenBytes

                val currentBytesOutPerSecond = txBytesMovingAvg.average().toLong()

                Logger.i(
                    TAG,
                    "dtx=$deltaTxBytes,dgen=$deltaGenBytes,etx=$estimatedRealDeltaTxBytes"
                )

                if (measureInterval <= previousQueueBytesOut.size) {
                    var countQueuedBytesGrowing = 0
                    for (i in 0 until previousQueueBytesOut.size - 1) {
                        if (previousQueueBytesOut[i] < previousQueueBytesOut[i + 1]) {
                            countQueuedBytesGrowing++
                        }
                    }
                    if (countQueuedBytesGrowing == measureInterval - 1) {
                        // insufficientBandwidth
                        rtmpStatsListener.onInsufficientBandwidth(
                            currentBytesOutPerSecond,
                            currentQueueBytesOut
                        )
                    } else if (countQueuedBytesGrowing == 0) {
                        rtmpStatsListener.onSufficientBandwidth(
                            currentBytesOutPerSecond,
                            currentQueueBytesOut
                        )
                    }
                    previousQueueBytesOut.removeFirst()
                    if (txBytesMovingAvg.isNotEmpty()) {
                        txBytesMovingAvg.removeFirst()
                    }
                }
                rtmpStatsListener.updateStats(
                    currentBytesOutPerSecond,
                    currentQueueBytesOut
                )

                // Schedule the next check
                if (isConnected && monitorConnectionId == connectionId) {
                    handler.postDelayed(this, 1000) // Adjust the delay as needed}
                }
            }
        }
        handler.post(dataUsageChecker)
    }

    companion object {
        private const val TAG = "CameraDualLiveStreamer"
    }
}
