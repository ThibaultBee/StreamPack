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
package io.github.thibaultbee.streampack.internal.sources.video.screen

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
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.orientation.AbstractSourceOrientationProvider
import io.github.thibaultbee.streampack.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

class ScreenSource(
    context: Context
) : IVideoSource {
    override var encoderSurface: Surface? = null
    override val timestampOffset = 0L
    override val hasSurface = true
    override val hasFrames = false
    override val orientationProvider = ScreenSourceOrientationProvider(context)

    override fun getFrame(buffer: ByteBuffer): Frame {
        throw UnsupportedOperationException("Screen source expects to run in Surface mode")
    }

    private var mediaProjection: MediaProjection? = null
    var activityResult: ActivityResult? = null

    private val _exception = MutableStateFlow<Exception?>(null)
    val exception: StateFlow<Exception?> = _exception

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
                runBlocking {
                    _exception.emit(StreamPackError("Screen source virtual display has been stopped"))
                }
            }
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Logger.i(TAG, "onStop")

            if (!isStoppedByUser) {
                runBlocking {
                    _exception.emit(StreamPackError("Screen source media projection has been stopped"))
                }
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

    override fun startStream() {
        require(videoConfig != null) { "Video has not been configured!" }
        require(activityResult != null) { "Activity result must be set!" }

        val resultCode = activityResult!!.resultCode
        val resultData = activityResult!!.data!!

        isStoppedByUser = false

        val orientedSize = orientationProvider.getOrientedSize(videoConfig!!.resolution)
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData).apply {
            virtualDisplay = createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                orientedSize.width,
                orientedSize.height,
                320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderSurface,
                virtualDisplayCallback,
                virtualDisplayHandler
            )
            registerCallback(mediaProjectionCallback, virtualDisplayHandler)
        }
    }


    override fun stopStream() {
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
}