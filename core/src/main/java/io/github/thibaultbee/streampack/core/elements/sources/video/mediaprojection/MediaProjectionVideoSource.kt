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
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.extensions.densityDpi
import io.github.thibaultbee.streampack.core.elements.utils.extensions.screenRect
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.extensions.getMediaProjection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MediaProjectionVideoSource(
    private val context: Context,
    val mediaProjection: MediaProjection
) : IVideoSourceInternal, ISurfaceSourceInternal, IMediaProjectionSource {
    override val timestampOffsetInNs = 0L
    override val infoProviderFlow =
        MutableStateFlow(FullScreenInfoProvider(context) as ISourceInfoProvider).asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private var outputSurface: Surface? = null

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

    override suspend fun configure(config: VideoSourceConfig) = Unit

    override suspend fun startStream() {
        val screenRect = context.screenRect

        mediaProjection.registerCallback(mediaProjectionCallback, virtualDisplayHandler)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            screenRect.width(),
            screenRect.height(),
            context.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            outputSurface,
            virtualDisplayCallback,
            virtualDisplayHandler
        )
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection.unregisterCallback(mediaProjectionCallback)
    }

    override fun release() {
        virtualDisplayThread.quitSafely()
        try {
            virtualDisplayThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private inner class FullScreenInfoProvider(private val context: Context) :
        DefaultSourceInfoProvider() {
        override fun getSurfaceSize(targetResolution: Size): Size {
            val screenRect = context.screenRect
            return Size(screenRect.width(), screenRect.height())
        }
    }

    companion object {
        private const val TAG = "ScreenSource"

        private const val VIRTUAL_DISPLAY_NAME = "StreamPackScreenSource"
    }
}

/**
 * A factory to create a [MediaProjectionVideoSourceFactory].
 *
 * @param mediaProjection The media projection
 */
class MediaProjectionVideoSourceFactory(private val mediaProjection: MediaProjection) :
    IVideoSourceInternal.Factory {
    override suspend fun create(context: Context): IVideoSourceInternal {
        val source = MediaProjectionVideoSource(context, mediaProjection)
        return source
    }

    override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
        return source is MediaProjectionVideoSource
    }
}