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
package io.github.thibaultbee.streampack.core.internal.sources.video.screen

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.orientation.AbstractSourceOrientationProvider
import io.github.thibaultbee.streampack.core.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.core.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.core.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer

class ScreenSource(
    context: Context
) : IVideoSource {
    override var outputSurface: Surface? = null
    override val timestampOffset = 0L
    override val hasOutputSurface = true
    override val hasFrames = false
    override val orientationProvider = ScreenSourceOrientationProvider(context)

    override fun getFrame(buffer: ByteBuffer): Frame {
        throw UnsupportedOperationException("Screen source expects to run in Surface mode")
    }

    private var mediaProjection: MediaProjection? = null
    var activityResult: ActivityResult? = null

    var listener: Listener? = null

    /**
     *  Avoid to trigger `onError` when screen source `stopStream` has been called.
     */
    private var isStoppedByUser = false

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null
    private var videoConfig: VideoConfig? = null
    private val virtualDisplayThread = HandlerThread("VirtualDisplayThread").apply { start() }
    private val virtualDisplayHandler = Handler(virtualDisplayThread.looper)
    private val virtualDisplayCallback = object : VirtualDisplay.Callback() {
        override fun onPaused() {
            super.onPaused()
            Logger.i(TAG, "onPaused")
        }

        override fun onStopped() {
            super.onStopped()
            Logger.i(TAG, "onStopped")

            if (!isStoppedByUser) {
                listener?.onStop()
            }
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Logger.i(TAG, "onStop")

            if (!isStoppedByUser) {
                listener?.onStop()
            }
        }
    }

    companion object {
        private const val TAG = "ScreenSource"

        private const val VIRTUAL_DISPLAY_NAME = "StreamPackScreenSource"
    }

    override fun configure(config: VideoConfig) {
        videoConfig = config
    }

    override suspend fun startStream() {
        val videoConfig = requireNotNull(videoConfig) { "Video has not been configured!" }
        val activityResult = requireNotNull(activityResult) { "Activity result must be set!" }

        val resultCode = activityResult.resultCode
        val resultData = activityResult.data!!

        isStoppedByUser = false

        val orientedSize = orientationProvider.getOrientedSize(videoConfig.resolution)
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData).apply {
            registerCallback(mediaProjectionCallback, virtualDisplayHandler)
            virtualDisplay = createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                orientedSize.width,
                orientedSize.height,
                320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                outputSurface,
                virtualDisplayCallback,
                virtualDisplayHandler
            )
        }
    }


    override suspend fun stopStream() {
        isStoppedByUser = true

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

    class ScreenSourceOrientationProvider(private val context: Context) :
        AbstractSourceOrientationProvider() {
        override val orientation = 0

        override fun getOrientedSize(size: Size): Size {
            return if (context.isDevicePortrait) {
                size.portraitize
            } else {
                size.landscapize
            }
        }
    }

    interface Listener {
        fun onStop()
    }
}