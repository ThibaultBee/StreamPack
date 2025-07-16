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
package io.github.thibaultbee.streampack.ext.media3.effect

import android.content.Context
import android.media.MediaFormat
import android.util.Size
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.DebugViewProvider
import androidx.media3.common.Effect
import androidx.media3.common.Format
import androidx.media3.common.SurfaceInfo
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.VideoFrameProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DefaultVideoFrameProcessor
import androidx.media3.effect.RgbFilter
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.ISurfaceOutput
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * Bitrate regulator configuration. Use it to control bitrate
 */
@UnstableApi
class Media3SurfaceProcessor internal constructor(
    private val context: Context, private val dynamicRangeProfile: DynamicRangeProfile
) : ISurfaceProcessorInternal {
    private val executor = Executors.newSingleThreadExecutor()

    private val wrappingListener = object : VideoFrameProcessor.Listener {
        override fun onInputStreamRegistered(
            inputType: Int,
            format: Format,
            effects: List<Effect>,
        ) {
            Logger.i(
                TAG,
                "Input stream registered with type: $inputType, format: $format, effects: $effects"
            )
        }

        override fun onError(exception: VideoFrameProcessingException) {
            Logger.e(TAG, "Error in Media3SurfaceProcessor", exception)
        }
    }
    private val processor = DefaultVideoFrameProcessor.Factory.Builder().build().create(
        context,
        DebugViewProvider.NONE,
        createColorInfo(dynamicRangeProfile),
        true,
        { },
        wrappingListener
    )

    private var surfaceInputsTimestampInNs: Long = 0L

    fun setEffects(effects: List<Effect>) {
        /*
        val format = Format.Builder().setColorInfo(createColorInfo(dynamicRangeProfile))
            .setWidth(1280) // TODO
            .setHeight(720) // TODO
            .build()
        processor.registerInputStream(
            VideoFrameProcessor.INPUT_TYPE_SURFACE_AUTOMATIC_FRAME_REGISTRATION,
            format,
            effects,
            surfaceInputsTimestampInNs
        )
        */
    }

    override fun createInputSurface(surfaceSize: Size, timestampOffsetInNs: Long): Surface {

        surfaceInputsTimestampInNs = timestampOffsetInNs
        val futureSurface = executor.submit<Surface> {
            val format = Format.Builder().setColorInfo(createColorInfo(dynamicRangeProfile))
                .setWidth(1280) // TODO
                .setHeight(720) // TODO
                .build()
            processor.registerInputStream(
                VideoFrameProcessor.INPUT_TYPE_SURFACE,//_AUTOMATIC_FRAME_REGISTRATION,
                format,
                listOf(RgbFilter.createGrayscaleFilter()),
                timestampOffsetInNs
            )
            Logger.i(
                TAG,
                "Creating input surface with size: $surfaceSize and timestamp offset: $timestampOffsetInNs"
            )

            val future = CompletableFuture<Surface>()
            processor.setOnInputSurfaceReadyListener {
                Logger.d(TAG, "Input surface is ready")
                future.complete(processor.inputSurface)
            }
            return@submit future.get()
        }
        val surface = futureSurface.get()
        Logger.i(TAG, "Returning input surface: $surface")
        return surface
    }

    override fun removeInputSurface(surface: Surface) {
        //TODO: Implement proper removal of input surface
    }

    override fun addOutputSurface(surfaceOutput: ISurfaceOutput) {
        executor.execute {
            processor.setOutputSurfaceInfo(
                SurfaceInfo(
                    surfaceOutput.descriptor.surface,
                    surfaceOutput.viewportRect.width(),
                    surfaceOutput.viewportRect.height(),
                    0, // TODO: No rotation
                    surfaceOutput.descriptor.isEncoderInputSurface
                )
            )
            Logger.i(TAG, "Added output surface: ${surfaceOutput.descriptor.surface}")
        }
    }

    override fun removeOutputSurface(surfaceOutput: ISurfaceOutput) {
        processor.setOutputSurfaceInfo(null)
    }

    override fun removeOutputSurface(surface: Surface) {
        processor.setOutputSurfaceInfo(null)
    }

    override fun removeAllOutputSurfaces() {
        processor.setOutputSurfaceInfo(null)
    }

    override fun release() {
        processor.release()
    }

    companion object {
        private const val TAG = "Media3SurfaceProcessor"

        private fun createColorInfo(dynamicRange: DynamicRangeProfile): ColorInfo {
            if (dynamicRange.isHdr) {
                val builder = ColorInfo.Builder().setColorRange(C.COLOR_RANGE_LIMITED)
                    .setColorSpace(C.COLOR_SPACE_BT2020)
                if (dynamicRange.transferFunction == MediaFormat.COLOR_TRANSFER_HLG) {
                    builder.setColorTransfer(C.COLOR_TRANSFER_HLG)
                } else {
                    builder.setColorTransfer(C.COLOR_TRANSFER_ST2084)
                }
                return builder.build()
            } else {
                return ColorInfo.Builder().setColorSpace(C.COLOR_SPACE_BT601)
                    .setColorRange(C.COLOR_RANGE_FULL).setColorTransfer(C.COLOR_TRANSFER_SDR)
                    .build()
            }
        }
    }
}

class Media3SurfaceProcessorFactory(
    private val context: Context,
) : ISurfaceProcessorInternal.Factory {
    @OptIn(UnstableApi::class)
    override fun create(dynamicRangeProfile: DynamicRangeProfile): ISurfaceProcessorInternal {
        return Media3SurfaceProcessor(context, dynamicRangeProfile)
    }
}
