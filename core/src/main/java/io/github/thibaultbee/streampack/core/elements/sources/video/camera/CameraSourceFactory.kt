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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider.Companion.THREAD_NAME_CAMERA
import io.github.thibaultbee.streampack.core.pipelines.IVideoDispatcherProvider

/**
 * Creates a [CameraSourceFactory] with the default camera.
 *
 * @param context the application context
 */
fun CameraSourceFactory(context: Context) =
    CameraSourceFactory(context.cameraManager.defaultCameraId)

/**
 * A factory to create a [CameraSource].
 *
 * @param cameraId the camera id to use.
 */
class CameraSourceFactory(val cameraId: String) : IVideoSourceInternal.Factory {
    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun create(
        context: Context,
        dispatcherProvider: IVideoDispatcherProvider
    ): IVideoSourceInternal {
        val cameraDispatcherProvider = CameraDispatcherProvider(
            dispatcherProvider.default,
            { dispatcherProvider.createVideoHandlerExecutor(THREAD_NAME_CAMERA) },
            { dispatcherProvider.createVideoExecutor(1, THREAD_NAME_CAMERA) }
        )

        return CameraSource(
            context,
            cameraId,
            cameraDispatcherProvider
        )
    }

    override fun isSourceEquals(source: IVideoSourceInternal?): Boolean {
        return source is CameraSource && source.cameraId == cameraId
    }

    override fun toString(): String {
        return "CameraSourceFactory(cameraId=$cameraId)"
    }
}