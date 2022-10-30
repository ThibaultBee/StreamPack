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
package io.github.thibaultbee.streampack.internal.sources.screen

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.sources.IVideoCapture
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.Logger
import java.nio.ByteBuffer

class ScreenCapture(
    context: Context
) : IVideoCapture {
    override var encoderSurface: Surface? = null
    override val timestampOffset = 0L
    override val hasSurface = true
    override fun getFrame(buffer: ByteBuffer): Frame {
        throw UnsupportedOperationException("Screen source expects to run in Surface mode")
    }

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
            Logger.i(this@ScreenCapture, "onPaused")
        }

        override fun onStopped() {
            super.onStopped()
            Logger.i(this@ScreenCapture, "onStopped")
            onErrorListener?.onError(StreamPackError("Screen capture has been stopped"))
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Logger.i(this@ScreenCapture, "onStop")
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