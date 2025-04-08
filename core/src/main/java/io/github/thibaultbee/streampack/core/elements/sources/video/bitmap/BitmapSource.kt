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

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.AbstractPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
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
internal class BitmapSource(override val bitmap: Bitmap) : AbstractPreviewableSource(),
    IVideoSourceInternal,
    ISurfaceSourceInternal,
    IBitmapSource {
    override val timestampOffsetInNs = 0L
    override val infoProviderFlow =
        MutableStateFlow(SourceInfoProvider() as ISourceInfoProvider).asStateFlow()

    private var videoSourceConfig: VideoSourceConfig? = null

    private var outputSurface: Surface? = null
    private var previewSurface: Surface? = null

    private val outputExecutor = Executors.newSingleThreadScheduledExecutor()
    private var outputScheduler: Future<*>? = null

    private val previewExecutor = Executors.newSingleThreadScheduledExecutor()
    private var previewScheduler: Future<*>? = null

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _isPreviewingFlow = MutableStateFlow(false)
    override val isPreviewingFlow = _isPreviewingFlow.asStateFlow()

    /**
     * Gets the size of the bitmap.
     */
    override fun <T> getPreviewSize(targetSize: Size, targetClass: Class<T>): Size {
        return Size(bitmap.width, bitmap.height)
    }

    override suspend fun hasPreview() = previewSurface != null

    override suspend fun setPreview(surface: Surface) {
        previewSurface = surface
    }

    override suspend fun startPreview() {
        requireNotNull(previewSurface) { "Preview surface must be set before starting preview" }

        _isPreviewingFlow.emit(true)
        previewScheduler = previewExecutor.scheduleWithFixedDelay(
            ::drawPreview,
            0,
            1000, // 1 frame per second. We don't need more for preview.
            TimeUnit.MILLISECONDS
        )
    }

    override suspend fun startPreview(previewSurface: Surface) {
        setPreview(previewSurface)
        startPreview()
    }

    override suspend fun stopPreview() {
        previewScheduler?.cancel(true)
        _isPreviewingFlow.emit(false)
    }

    override suspend fun resetPreviewImpl() {
        stopPreview()
        previewSurface = null
    }

    override suspend fun getOutput(): Surface? = outputSurface
    override suspend fun setOutput(surface: Surface) {
        outputSurface = surface
    }

    override suspend fun resetOutputImpl() {
        stopStream()
        outputSurface = null
    }

    override suspend fun configure(config: VideoSourceConfig) {
        videoSourceConfig = config
    }

    override suspend fun startStream() {
        val sourceConfig =
            requireNotNull(videoSourceConfig) { "Video source must be configured before starting stream" }
        requireNotNull(outputSurface) { "Output surface must be set before starting stream" }

        _isStreamingFlow.emit(true)
        outputScheduler = outputExecutor.scheduleWithFixedDelay(
            ::drawOutput,
            0,
            1000 / sourceConfig.fps.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    override suspend fun stopStream() {
        outputScheduler?.cancel(true)
        _isStreamingFlow.emit(false)
    }

    override fun release() {
        outputExecutor.shutdown()
        previewExecutor.shutdown()
    }

    private fun drawOutput() {
        outputSurface?.let {
            bitmap.let { bitmap ->
                val canvas = it.lockCanvas(null)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                it.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun drawPreview() {
        previewSurface?.let {
            val canvas = it.lockCanvas(null)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            it.unlockCanvasAndPost(canvas)
        }
    }

    private inner class SourceInfoProvider : DefaultSourceInfoProvider(rotationDegrees = 0) {
        override fun getSurfaceSize(targetResolution: Size): Size {
            return Size(bitmap.width, bitmap.height)
        }
    }
}

/**
 * A factory to create a [BitmapSource].
 *
 * @param bitmap the [Bitmap] to stream.
 */
class BitmapSourceFactory(private val bitmap: Bitmap) : IVideoSourceInternal.Factory {
    override suspend fun create(context: Context): IVideoSourceInternal {
        return BitmapSource(bitmap)
    }

    override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
        return source is BitmapSource && source.bitmap == bitmap
    }
}