/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.processing.video

import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.ISurfaceOutput
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile

interface ISurfaceProcessor

/**
 * Interface for a surface processor that manages input and output surfaces.
 *
 * This interface allows for the creation, removal, and management of input and output surfaces
 * used in video processing.
 *
 * You can create your own implementation of this interface to handle custom effects or processing.
 */
interface ISurfaceProcessorInternal : ISurfaceProcessor, Releasable {
    fun createInputSurface(surfaceSize: Size, timestampOffsetInNs: Long): Surface

    fun removeInputSurface(surface: Surface)

    fun addOutputSurface(surfaceOutput: ISurfaceOutput)

    fun removeOutputSurface(surfaceOutput: ISurfaceOutput)

    fun removeOutputSurface(surface: Surface)

    fun removeAllOutputSurfaces()

    /**
     * Factory interface for creating instances of [ISurfaceProcessorInternal].
     */
    interface Factory {
        fun create(dynamicRangeProfile: DynamicRangeProfile): ISurfaceProcessorInternal
    }
}