package com.github.thibaultbee.srtstreamer

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.srtstreamer.encoders.AudioEncoder
import com.github.thibaultbee.srtstreamer.encoders.IEncoderListener
import com.github.thibaultbee.srtstreamer.encoders.VideoEncoder
import com.github.thibaultbee.srtstreamer.endpoints.IEndpoint
import com.github.thibaultbee.srtstreamer.listeners.OnErrorListener
import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.mux.IMuxListener
import com.github.thibaultbee.srtstreamer.mux.ts.TSMux
import com.github.thibaultbee.srtstreamer.mux.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.sources.AudioCapture
import com.github.thibaultbee.srtstreamer.sources.CameraCapture
import com.github.thibaultbee.srtstreamer.utils.DeviceOrientation
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer


class Streamer(val endpoint: IEndpoint, val logger: Logger) : EventHandlerManager() {
    override var onErrorListener: OnErrorListener? = null
        set(value) {
            audioSource.onErrorListener = value
            videoSource.onErrorListener = value
            audioEncoder.onErrorListener = value
            videoEncoder.onErrorListener = value
            field = value
        }
    var context: Context? = null
        set(value) {
            if (value != null) {
                videoSource.context = value
            }
            field = value
        }

    private val audioEncoder =
        AudioEncoder(logger)
    private val videoEncoder =
        VideoEncoder(logger)

    private val audioSource =
        AudioCapture(logger)
    val videoSource =
        CameraCapture(logger)

    private val serviceInfo = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "MyService",
        "MyProvider"
    )
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
    private val tsMux = TSMux(muxListener, serviceInfo)

    var videoBitrate = 0
        set(value) {
            videoEncoder.bitrate = value
            field = value
        }

    private var audioTsStreamId: Short? = null
    private var videoTsStreamId: Short? = null

    // Keep video configuration
    private lateinit var videoConfig: VideoConfig
    private lateinit var audioConfig: AudioConfig


    private var videoBaseTimestamp = -1L
    private var audioBaseTimestamp = -1L

    private val audioEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame? {
            return audioSource.getFrame(buffer)
        }

        override fun onOutputFrame(frame: Frame): Error {
            // Drop codec data
            if (frame.isCodecData) {
                return Error.SUCCESS
            }

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
                    return Error.UNKNOWN
                }
            }

            return Error.SUCCESS
        }
    }

    fun configureAudio(mimeType: String = MediaFormat.MIMETYPE_AUDIO_AAC, startBitrate: Int = 128000, sampleRate: Int = 441000, channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO, audioBitFormat: Int = AudioFormat.ENCODING_PCM_16BIT): Error {
        audioConfig = AudioConfig(mimeType, startBitrate, sampleRate, channelConfig, audioBitFormat)
        val error = audioSource.configure(sampleRate, channelConfig, audioBitFormat)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to set audio capture")
            return error
        }

        val nChannel = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 1
        }
        return audioEncoder.configure(mimeType, startBitrate, sampleRate, nChannel, audioBitFormat)
    }

    private val videoEncoderListener = object : IEncoderListener {
        override fun onInputFrame(buffer: ByteBuffer): Frame {
            // Not needed for video
            TODO("Not yet implemented")
        }

        override fun onOutputFrame(frame: Frame): Error {
            // Drop codec data
            if (frame.isCodecData) {
                return Error.SUCCESS
            }

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
            return Error.SUCCESS
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun configureVideo(mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC, startBitrate: Int = 2000000, resolution: Size = Size(1920,1080), fps: Int = 30): Error {
        // Keep settings, in case we need to reconfigure
        videoConfig = VideoConfig(mimeType, startBitrate, resolution, fps)

        val error = videoSource.configure(fps)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to set camera configuration")
            return error
        }

        val orientation = if (context != null) {
            DeviceOrientation.get(context!!)
        } else {
            logger.w(this, "Failed to get context: set rotation to default only")
            90
        }

        return videoEncoder.configure(mimeType, startBitrate, resolution, fps, orientation)
    }

    private fun configureVideo(): Error {
        return videoEncoder.configure(
            videoConfig.mimeType,
            videoConfig.startBitrate,
            videoConfig.resolution,
            videoConfig.fps,
            DeviceOrientation.get(context!!)
        )
    }


    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface, cameraId: String = "0"): Error {
        videoSource.previewSurface = previewSurface
        val encoderSurface = videoEncoder.getIntputSurface()
        if (encoderSurface == null) {
            logger.e(this, "Surface can't be null")
            return Error.BAD_STATE
        }
        videoSource.encoderSurface = encoderSurface
        val error = videoSource.startPreview(cameraId)
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to start video preview")
            return error
        }

        return audioSource.start()
    }

    fun stopCapture() {
        videoSource.stopPreview()

        audioSource.stop()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun changeVideoSource(cameraId: String = "0"): Error {
        videoSource.stopPreview()

        return videoSource.startPreview(cameraId)
    }

    fun startStream(): Error {
        endpoint.run()

        audioEncoder.encoderListener = audioEncoderListener
        videoEncoder.encoderListener = videoEncoderListener

        val streams = mutableListOf<String>()
        videoEncoder.getMimeType()?.let { streams.add(it) }
        audioEncoder.getMimeType()?.let { streams.add(it) }

        tsMux.addStreams(serviceInfo, streams)
        videoEncoder.getMimeType()?.let { videoTsStreamId = tsMux.getStreams(it)[0].pid }
        audioEncoder.getMimeType()?.let { audioTsStreamId = tsMux.getStreams(it)[0].pid }

        val error = audioEncoder.start()
        if (error != Error.SUCCESS) {
            logger.e(this, "Failed to start audio encoder")
            return error
        }

        videoSource.startStream()

        return videoEncoder.start()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        audioEncoder.encoderListener = null
        videoEncoder.encoderListener = null

        videoSource.stopStream()
        videoEncoder.stop()
        audioEncoder.stop()

        tsMux.stop()

        endpoint.stop()

        // Encoder does not return to CONFIGURED state... so we have to reset everything for video...
        resetAudio()
        resetVideo()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun resetAudio(): Error {
        audioBaseTimestamp = -1L

        audioEncoder.release()

        // Reconfigure
        val nChannel = when (audioConfig.channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 1
        }
        return audioEncoder.configure(audioConfig.mimeType, audioConfig.startBitrate, audioConfig.sampleRate, nChannel, audioConfig.audioBitFormat)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun resetVideo(): Error {
        videoBaseTimestamp = -1L

        videoEncoder.release()
        videoEncoder.getIntputSurface()?.release()
        videoSource.stopPreview()

        // Reconfigure
        configureVideo()

        // And restart...
        val encoderSurface = videoEncoder.getIntputSurface()
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
        audioEncoder.release()
        videoEncoder.release()
        audioSource.release()
        endpoint.close()
    }

    data class VideoConfig(var mimeType: String, var startBitrate: Int, var resolution: Size, var fps: Int)
    data class AudioConfig(var mimeType: String, var startBitrate: Int, var sampleRate: Int, var channelConfig: Int, var audioBitFormat: Int)
}