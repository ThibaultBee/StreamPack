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
import android.graphics.Color
import android.graphics.Paint
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.AbstractPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.time.Timebase
import io.github.thibaultbee.streampack.core.pipelines.IVideoDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Video source that streams a [Bitmap].
 */
// TODO: move to coroutines instead of ExecutorService
internal class BitmapSource(override val bitmap: Bitmap) : AbstractPreviewableSource(),
    IVideoSourceInternal,
    ISurfaceSourceInternal,
    IBitmapSource {
    override val timebase = Timebase.UPTIME
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
        require(!config.dynamicRangeProfile.isHdr) {
            "Bitmap source does not support HDR"
        }
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

    override suspend fun release() {
        // Stop any scheduled tasks first
        outputScheduler?.cancel(true)
        previewScheduler?.cancel(true)
        
        // Shutdown executors and wait for them to finish
        outputExecutor.shutdown()
        previewExecutor.shutdown()
        try {
            // Wait for executors to finish any in-progress drawing
            outputExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
            previewExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        // Now safe to recycle pre-composited bitmaps
        compositedBitmaps.forEach { it.recycle() }
        compositedBitmaps.clear()
    }

    // Pre-composited bitmaps (source + noise combined) for maximum performance
    // Single drawBitmap call per frame instead of two
    private val compositedBitmaps = mutableListOf<Bitmap>()
    private var frameIndex = 0
    // Number of pre-composited frames to cycle through
    private val frameCount = 4
    
    private val noisePaint = Paint().apply {
        alpha = 255 // Full opacity noise for maximum entropy
        strokeWidth = 3f // Draw larger points for more visual noise
    }
    
    /**
     * Generate pre-composited bitmaps (source bitmap + heavy noise overlay).
     * Heavy noise is intentional to prevent encoder from compressing too efficiently,
     * which keeps bitrate closer to target. This helps avoid OBS scene switchers
     * that detect "offline" streams when bitrate drops too low on static content.
     */
    private fun ensureCompositedBitmapsGenerated() {
        if (compositedBitmaps.isNotEmpty()) return
        
        val width = bitmap.width
        val height = bitmap.height
        // Use 10,000 noise points - fast to generate
        // With strokeWidth=3, each point covers ~9 pixels for decent coverage
        val noisePointCount = 10000
        
        for (i in 0 until frameCount) {
            // Create a copy of the source bitmap with noise baked in
            val composited = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: continue
            val canvas = android.graphics.Canvas(composited)
            
            // Draw noise points directly onto the copy
            // Using full color range for maximum entropy
            for (j in 0 until noisePointCount) {
                val x = Random.nextInt(width).toFloat()
                val y = Random.nextInt(height).toFloat()
                // Full RGB noise for more entropy
                val r = Random.nextInt(256)
                val g = Random.nextInt(256)
                val b = Random.nextInt(256)
                noisePaint.color = Color.rgb(r, g, b)
                canvas.drawPoint(x, y, noisePaint)
            }
            
            compositedBitmaps.add(composited)
        }
    }

    private fun drawOutput() {
        outputSurface?.let { surface ->
            try {
                ensureCompositedBitmapsGenerated()
                
                val canvas = surface.lockCanvas(null) ?: return@let
                
                // Single drawBitmap call - source and noise pre-composited
                val bitmapToDraw = if (compositedBitmaps.isNotEmpty()) {
                    compositedBitmaps.getOrNull(frameIndex % compositedBitmaps.size)?.takeIf { !it.isRecycled }
                } else {
                    bitmap.takeIf { !it.isRecycled }
                }
                
                if (bitmapToDraw != null) {
                    canvas.drawBitmap(bitmapToDraw, 0f, 0f, null)
                    frameIndex++
                }
                
                surface.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                // Ignore drawing errors (bitmap recycled, surface invalid, etc.)
            }
        }
    }

    private fun drawPreview() {
        previewSurface?.let {
            try {
                if (bitmap.isRecycled) return@let
                val canvas = it.lockCanvas(null) ?: return@let
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                it.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                // Ignore drawing errors
            }
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
    override suspend fun create(
        context: Context,
        dispatcherProvider: IVideoDispatcherProvider
    ): IVideoSourceInternal {
        return BitmapSource(bitmap)
    }

    override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
        return source is BitmapSource && source.bitmap == bitmap
    }

    override fun toString(): String {
        return "BitmapSourceFactory(bitmap=$bitmap)"
    }
}