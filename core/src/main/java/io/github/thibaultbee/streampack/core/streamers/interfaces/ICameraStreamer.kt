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
package io.github.thibaultbee.streampack.core.streamers.interfaces

import android.view.Surface
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.ICameraSource

interface ICameraStreamer {
    /**
     * The camera source settings.
     */
    val videoSource: ICameraSource

    /**
     * Gets/Sets current camera id.
     * It is a shortcut for [videoSource.cameraId].
     */
    var camera: String

    /**
     * Sets a preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    fun setPreview(surface: Surface)

    /**
     * Stops camera preview.
     */
    fun stopPreview()
}

interface ICameraCoroutineStreamer : ICameraStreamer {
    /**
     * Starts audio and video capture.
     */
    suspend fun startPreview()
}

interface ICameraCallbackStreamer : ICameraStreamer {
    /**
     * Starts audio and video capture.
     */
    fun startPreview()
}