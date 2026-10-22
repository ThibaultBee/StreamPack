/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.pipelines.outputs

import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A video surface output for a streamer pipeline.
 *
 * It provides a [SurfaceDescriptor] to render video frames directly to a [Surface] from the
 * pipeline without encoding.
 *
 * @param defaultRotation The default target rotation.
 */
@InternalStreamPackApi
class SurfacePipelineOutput(
    @RotationValue defaultRotation: Int = Surface.ROTATION_0
) : IVideoSurfacePipelineOutputInternal {
    override val withAudio: Boolean = false
    override val withVideo: Boolean = true

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow: StateFlow<Throwable?> = _throwableFlow.asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow: StateFlow<Boolean> = _isStreamingFlow.asStateFlow()

    private val _surfaceFlow = MutableStateFlow<SurfaceDescriptor?>(null)
    override val surfaceFlow: StateFlow<SurfaceDescriptor?> = _surfaceFlow.asStateFlow()

    private var _targetRotation: Int = defaultRotation
    override val targetRotation: Int
        get() = _targetRotation

    /**
     * Sets the [SurfaceDescriptor] for this output.
     *
     * @param surfaceDescriptor The [SurfaceDescriptor] or null to clear.
     */
    fun setSurface(surfaceDescriptor: SurfaceDescriptor?) {
        surfaceDescriptor?.let {
            _targetRotation = it.targetRotation
        }
        _surfaceFlow.value = surfaceDescriptor
    }

    override suspend fun startStream() {
        if (_isStreamingFlow.value) {
            return
        }
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        if (!_isStreamingFlow.value) {
            return
        }
        _isStreamingFlow.emit(false)
    }

    override suspend fun release() {
        stopStream()
        _surfaceFlow.emit(null)
    }
}
