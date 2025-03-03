/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video.bitmap

import android.graphics.Bitmap
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Video source that streams a bitmap.
 *
 * Only for testing purpose.
 */
class BitmapSource : IVideoSourceInternal, ISurfaceSource, IBitmapSource {
    override var outputSurface: Surface? = null
    override val timestampOffset = 0L
    override val infoProviderFlow = MutableStateFlow(DefaultSourceInfoProvider()).asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private var videoSourceConfig: VideoSourceConfig? = null
    override var bitmap: Bitmap? = null

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var scheduler: Future<*>? = null

    override fun configure(config: VideoSourceConfig) {
        videoSourceConfig = config
    }

    override suspend fun startStream() {
        val sourceConfig =
            requireNotNull(videoSourceConfig) { "Video source must be configured before starting stream" }
        requireNotNull(outputSurface) { "Output surface must be set before starting stream" }
        requireNotNull(bitmap) { "Bitmap must be set before starting stream" }

        _isStreamingFlow.emit(true)
        scheduler = executor.scheduleWithFixedDelay(
            ::drawBitmap,
            0,
            1000 / sourceConfig.fps.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    override suspend fun stopStream() {
        scheduler?.cancel(true)
        _isStreamingFlow.emit(false)
    }

    override fun release() {
        executor.shutdown()
    }

    private fun drawBitmap() {
        outputSurface?.let {
            bitmap?.let { bitmap ->
                val canvas = it.lockCanvas(null)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                it.unlockCanvasAndPost(canvas)
            }
        }
    }
}