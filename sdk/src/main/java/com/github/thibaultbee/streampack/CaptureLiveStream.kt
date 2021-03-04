package com.github.thibaultbee.streampack

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.Frame
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.encoders.AudioMediaCodecEncoder
import com.github.thibaultbee.streampack.encoders.IEncoder
import com.github.thibaultbee.streampack.encoders.IEncoderListener
import com.github.thibaultbee.streampack.encoders.VideoMediaCodecEncoder
import com.github.thibaultbee.streampack.endpoints.IEndpoint
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.muxers.IMuxerListener
import com.github.thibaultbee.streampack.muxers.ts.TSMuxer
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.sources.AudioCapture
import com.github.thibaultbee.streampack.sources.CameraCapture
import com.github.thibaultbee.streampack.utils.Error
import com.github.thibaultbee.streampack.utils.EventHandlerManager
import com.github.thibaultbee.streampack.utils.Logger
import java.nio.ByteBuffer
import java.security.InvalidParameterException


class CaptureLiveStream(
    private val context: Context,
    private val tsServiceInfo: ServiceInfo,
    private val endpoint: IEndpoint,
    val logger: Logger
) : EventHandlerManager() {
    override var onErrorListener: OnErrorListener? = null

    private var audioEncoder: IEncoder? = null
    private var videoEncoder: VideoMediaCodecEncoder? = null

    private val audioSource = AudioCapture(logger)
    var videoSource: CameraCapture? = null

    private val muxListener = object : IMuxerListener {
        override fun onOutputFrame(buffer: ByteBuffer) {
            try {
                endpoint.write(buffer)
            } catch (e: Exception) {
                stopStream()
                reportConnectionLost()
            }
        }
    }
    private val tsMux = TSMuxer(muxListener)

    private var audioTsStreamId: Short? = null
    private var videoTsStreamId: Short? = null

    // Keep video configuration
    private var videoConfig: VideoConfig? = null
    private var audioConfig: AudioConfig? = null


    private var videoBaseTimestamp = -1L
    private var audioBaseTimestamp = -1L

    private val onCodecErrorListener = object : OnErrorListener {
        override fun onError(name: String, type: Error) {
            stopStream()
            onErrorListener?.onError(name, type)
        }
    }

    private val onCaptureErrorListener = object : OnErrorListener {
        override fun onError(name: String, type: Error) {
            stopStream()
            stopCapture()
            onErrorListener?.onError(name, type)
        }
    }

    private val audioEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            return audioSource.getFrame(buffer)
        }

        override fun onOutputFrame(frame: Frame) {
            /*
             * In case device is >= N, we got real audio timestamp from bootime.
             * They can be compare with video timestamp
             */
            val baseTimestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (videoBaseTimestamp == -1L) {
                    videoBaseTimestamp = frame.pts
                }
                videoBaseTimestamp
            } else {
                if (audioBaseTimestamp == -1L) {
                    audioBaseTimestamp = frame.pts
                }
                audioBaseTimestamp
            }

            frame.pts -= baseTimestamp
            frame.dts?.let { it - baseTimestamp }

            audioTsStreamId?.let {
                try {
                    tsMux.encode(frame, it)
                } catch (e: Exception) {
                    reportError(e)
                }
            }
        }
    }

    private val videoEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            // Not needed for video
            TODO("Not yet implemented")
        }

        override fun onOutputFrame(frame: Frame) {
            if (videoBaseTimestamp == -1L) {
                videoBaseTimestamp = frame.pts
            }

            frame.pts -= videoBaseTimestamp
            frame.dts?.let { it - videoBaseTimestamp }

            videoTsStreamId?.let {
                try {
                    tsMux.encode(frame, it)
                } catch (e: Exception) {
                    reportError(e)
                }
            }
        }
    }

    fun configure(audioConfig: AudioConfig) {
        this.audioConfig = audioConfig

        audioSource.set(audioConfig)
        audioEncoder =
            AudioMediaCodecEncoder(audioConfig, audioEncoderListener, onCodecErrorListener, logger)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun configure(videoConfig: VideoConfig) {
        // Keep settings when we need to reconfigure
        this.videoConfig = videoConfig

        videoSource = CameraCapture(context, videoConfig.fps, onCaptureErrorListener, logger)
        videoEncoder =
            VideoMediaCodecEncoder(videoConfig, videoEncoderListener, onCodecErrorListener, logger)
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface, cameraId: String = "0"): Error {
        require(audioConfig != null)
        require(videoEncoder != null)
        require(videoSource != null)

        videoSource!!.previewSurface = previewSurface
        val encoderSurface = videoEncoder!!.intputSurface
        videoSource!!.encoderSurface = encoderSurface
        val error = videoSource!!.startPreview(cameraId)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to start video preview")
            return error
        }

        audioSource.run()
        return Error.SUCCESS
    }

    fun stopCapture() {
        videoSource?.stopPreview()
        audioSource.stop()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun changeVideoSource(cameraId: String = "0"): Error {
        require(videoSource != null)

        videoSource!!.stopPreview()
        return videoSource!!.startPreview(cameraId)
    }

    @Throws
    fun startStream() {
        require(videoEncoder != null)
        require(audioEncoder != null)
        require(videoSource != null)

        endpoint.run()

        val streams = mutableListOf<String>()
        videoEncoder!!.mimeType?.let { streams.add(it) }
        audioEncoder!!.mimeType?.let { streams.add(it) }

        tsMux.addService(tsServiceInfo)
        tsMux.addStreams(tsServiceInfo, streams)
        videoEncoder!!.mimeType?.let { videoTsStreamId = tsMux.getStreams(it)[0].pid }
        audioEncoder!!.mimeType?.let { audioTsStreamId = tsMux.getStreams(it)[0].pid }

        audioEncoder!!.run()

        videoSource!!.startStream()

        videoEncoder!!.run()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        videoSource?.stopStream()
        videoEncoder?.stop()
        audioEncoder?.stop()

        tsMux.stop()

        endpoint.stop()

        // Encoder does not return to CONFIGURED state... so we have to reset everything for video...
        resetAudio()
        resetVideo()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun resetAudio(): Error {
        require(audioConfig != null)

        audioBaseTimestamp = -1L

        audioEncoder?.close()

        // Reconfigure
        audioEncoder = AudioMediaCodecEncoder(
            audioConfig!!,
            audioEncoderListener,
            onCodecErrorListener,
            logger
        )
        return Error.SUCCESS
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun resetVideo() {
        require(videoConfig != null)

        videoBaseTimestamp = -1L

        videoEncoder?.intputSurface?.release()
        videoEncoder?.close()
        videoSource?.stopPreview()

        // And restart...
        videoEncoder = VideoMediaCodecEncoder(
            videoConfig!!,
            videoEncoderListener,
            onCodecErrorListener,
            logger
        )
        val encoderSurface = videoEncoder?.intputSurface
            ?: throw InvalidParameterException("Surface can't be null")
        videoSource?.encoderSurface = encoderSurface
        videoSource?.startPreview()
    }

    fun release() {
        audioEncoder?.close()
        audioEncoder = null
        videoEncoder?.close()
        videoEncoder = null
        audioSource.close()
        endpoint.close()
    }
}