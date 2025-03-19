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
package io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.extensions.densityDpi
import io.github.thibaultbee.streampack.core.elements.utils.extensions.screenRect
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MediaProjectionVideoSource(
    private val context: Context
) : IVideoSourceInternal, ISurfaceSourceInternal, IMediaProjectionSource {
    override val timestampOffsetInNs = 0L
    override val infoProviderFlow = MutableStateFlow(DefaultSourceInfoProvider()).asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private var outputSurface: Surface? = null

    private var mediaProjection: MediaProjection? = null

    /**
     * Set the activity result to get the media projection.
     */
    override var activityResult: ActivityResult? = null

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var virtualDisplay: VirtualDisplay? = null

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

            _isStreamingFlow.tryEmit(false)
        }
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Logger.i(TAG, "onStop")

            _isStreamingFlow.tryEmit(false)
        }
    }

    override suspend fun getOutput() = outputSurface
    override suspend fun setOutput(surface: Surface) {
        outputSurface = surface
    }

    override suspend fun resetOutput() {
        stopStream()
        outputSurface = null
    }

    override fun configure(config: VideoSourceConfig) = Unit

    override suspend fun startStream() {
        val activityResult = requireNotNull(activityResult) {
            "MediaProjection requires an activity result to be set"
        }

        val screenRect = context.screenRect

        mediaProjection = mediaProjectionManager.getMediaProjection(
            activityResult.resultCode,
            activityResult.data!!
        ).apply {
            registerCallback(mediaProjectionCallback, virtualDisplayHandler)
            virtualDisplay = createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenRect.width(),
                screenRect.height(),
                context.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                outputSurface,
                virtualDisplayCallback,
                virtualDisplayHandler
            )
        }
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
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

    companion object {
        private const val TAG = "ScreenSource"

        private const val VIRTUAL_DISPLAY_NAME = "StreamPackScreenSource"
    }
}

/**
 * A factory to create a [MediaProjectionVideoSource].
 */
class MediaProjectionVideoSourceFactory : IVideoSourceInternal.Factory {
    override suspend fun create(context: Context): IVideoSourceInternal =
        MediaProjectionVideoSource(context)

    override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
        return source is MediaProjectionVideoSource
    }
}