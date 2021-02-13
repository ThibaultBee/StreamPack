package com.github.thibaultbee.srtstreamer

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.srtstreamer.data.AudioConfig
import com.github.thibaultbee.srtstreamer.data.Frame
import com.github.thibaultbee.srtstreamer.data.VideoConfig
import com.github.thibaultbee.srtstreamer.encoders.AudioMediaCodecEncoder
import com.github.thibaultbee.srtstreamer.encoders.IEncoder
import com.github.thibaultbee.srtstreamer.encoders.IEncoderListener
import com.github.thibaultbee.srtstreamer.encoders.VideoMediaCodecEncoder
import com.github.thibaultbee.srtstreamer.endpoints.IEndpoint
import com.github.thibaultbee.srtstreamer.listeners.OnErrorListener
import com.github.thibaultbee.srtstreamer.mux.IMuxListener
import com.github.thibaultbee.srtstreamer.mux.ts.TSMux
import com.github.thibaultbee.srtstreamer.mux.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.sources.AudioCapture
import com.github.thibaultbee.srtstreamer.sources.CameraCapture
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer


class Streamer(
    private val tsServiceInfo: ServiceInfo,
    private val endpoint: IEndpoint,
    val logger: Logger
) : EventHandlerManager() {
    override var onErrorListener: OnErrorListener? = null
        set(value) {
            videoSource.onErrorListener = value
            field = value
        }
    var context: Context? = null
        set(value) {
            if (value != null) {
                videoSource.context = value
            }
            field = value
        }

    private var audioEncoder: IEncoder? = null
    private var videoEncoder: VideoMediaCodecEncoder? = null

    private var audioSource: AudioCapture? = null
    val videoSource =
        CameraCapture(logger)

    private val muxListener = object : IMuxListener {
        override fun onOutputFrame(buffer: ByteBuffer) {
            try {
                endpoint.write(buffer)
            } catch (e: Exception) {
                stopStream()
                reportConnectionLost()
            }
        }
    }
    private val tsMux = TSMux(muxListener)

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

    private val audioEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            require(audioSource != null)

            return audioSource!!.getFrame(buffer)
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

        audioSource = AudioCapture(audioConfig, logger)
        audioEncoder =
            AudioMediaCodecEncoder(audioConfig, audioEncoderListener, onCodecErrorListener, logger)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun configure(videoConfig: VideoConfig): Error {
        // Keep settings, in case we need to reconfigure
        this.videoConfig = videoConfig

        val error = videoSource.configure(videoConfig.fps)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to set camera configuration")
            return error
        }

        videoEncoder =
            VideoMediaCodecEncoder(videoConfig, videoEncoderListener, onCodecErrorListener, logger)
        return Error.SUCCESS
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface, cameraId: String = "0"): Error {
        require(videoEncoder != null)
        require(audioSource != null)

        videoSource.previewSurface = previewSurface
        val encoderSurface = videoEncoder!!.intputSurface
        videoSource.encoderSurface = encoderSurface
        val error = videoSource.startPreview(cameraId)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to start video preview")
            return error
        }

        audioSource!!.run()
        return Error.SUCCESS
    }

    fun stopCapture() {
        videoSource.stopPreview()

        audioSource?.stop()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun changeVideoSource(cameraId: String = "0"): Error {
        videoSource.stopPreview()

        return videoSource.startPreview(cameraId)
    }

    @Throws
    fun startStream() {
        require(videoEncoder != null)
        require(audioEncoder != null)

        endpoint.run()

        val streams = mutableListOf<String>()
        videoEncoder!!.mimeType?.let { streams.add(it) }
        audioEncoder!!.mimeType?.let { streams.add(it) }

        tsMux.addService(tsServiceInfo)
        tsMux.addStreams(tsServiceInfo, streams)
        videoEncoder!!.mimeType?.let { videoTsStreamId = tsMux.getStreams(it)[0].pid }
        audioEncoder!!.mimeType?.let { audioTsStreamId = tsMux.getStreams(it)[0].pid }

        audioEncoder!!.run()

        videoSource.startStream()

        videoEncoder!!.run()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        videoSource.stopStream()
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
    private fun resetVideo(): Error {
        require(videoConfig != null)

        videoBaseTimestamp = -1L

        videoEncoder?.intputSurface?.release()
        videoEncoder?.close()
        videoSource.stopPreview()

        // And restart...
        videoEncoder = VideoMediaCodecEncoder(
            videoConfig!!,
            videoEncoderListener,
            onCodecErrorListener,
            logger
        )
        val encoderSurface = videoEncoder?.intputSurface
        if (encoderSurface == null) {
            logger.e(this, "Surface can't be null")
            return Error.BAD_STATE
        }
        videoSource.encoderSurface = encoderSurface
        val error = videoSource.startPreview()
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to start video preview")
            return error
        }

        return Error.SUCCESS
    }

    fun release() {
        audioEncoder?.close()
        audioEncoder = null
        videoEncoder?.close()
        videoEncoder = null
        audioSource?.close()
        audioSource = null
        endpoint.close()
    }
}