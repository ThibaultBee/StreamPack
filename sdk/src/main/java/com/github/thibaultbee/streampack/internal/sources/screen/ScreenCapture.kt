package com.github.thibaultbee.streampack.internal.sources.screen

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.activity.result.ActivityResult
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.internal.sources.ISurfaceCapture
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.logger.ILogger

class ScreenCapture(
    context: Context,
    logger: ILogger
) : ISurfaceCapture<VideoConfig> {
    override var encoderSurface: Surface? = null
    override val timestampOffset = 0L
    private var mediaProjection: MediaProjection? = null
    var activityResult: ActivityResult? = null
    var onErrorListener: OnErrorListener? = null

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null
    private var videoConfig: VideoConfig? = null
    private val virtualDisplayThread = HandlerThread("VirtualDisplayThread").apply { start() }
    private val virtualDisplayHandler = Handler(virtualDisplayThread.looper)
    private val virtualDisplayCallback = object : VirtualDisplay.Callback() {
        override fun onPaused() {
            super.onPaused()
            logger.i(this@ScreenCapture, "onPaused")
        }

        override fun onStopped() {
            super.onStopped()
            logger.i(this@ScreenCapture, "onStopped")
            onErrorListener?.onError(StreamPackError("Screen capture has been stopped"))
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            logger.i(this@ScreenCapture, "onStop")
            onErrorListener?.onError(StreamPackError("Screen capture has been stopped"))
        }
    }

    companion object {
        private const val VIRTUAL_DISPLAY_NAME = "StreamPackScreenCapture"
    }

    override fun configure(config: VideoConfig) {
        videoConfig = config
    }

    override fun startStream() {
        require(videoConfig != null) { "Video has not been configured!" }
        require(activityResult != null) { "Activity result must be set!" }

        mediaProjection = activityResult?.let {
            mediaProjectionManager.getMediaProjection(it.resultCode, it.data!!)
        }

        videoConfig?.let {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                it.resolution.width,
                it.resolution.height,
                320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderSurface,
                virtualDisplayCallback,
                virtualDisplayHandler
            )
            mediaProjection?.registerCallback(mediaProjectionCallback, virtualDisplayHandler)
        }
    }


    override fun stopStream() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun release() {
        virtualDisplayThread.quitSafely()
        try {
            virtualDisplayThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}