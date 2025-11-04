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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

import android.os.Handler
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * Data class that provides dispatchers, handlers, and executors for camera operations.
 *
 * @property default The default [CoroutineDispatcher] for camera operations.
 * @property cameraHandlerBuilder A lambda that builds and returns a [Handler] for camera operations.
 * @property cameraExecutorBuilder A lambda that builds and returns an [Executor] for camera operations
 */
internal data class CameraDispatcherProvider(
    val default: CoroutineDispatcher,

    private val cameraHandlerBuilder: () -> HandlerThreadExecutor,
    private val cameraExecutorBuilder: () -> ExecutorService,
) {
    private val _camera2Handler by lazy { cameraHandlerBuilder() }
    private val _camera2Executor by lazy { cameraExecutorBuilder() }

    /**
     * Handler for camera operations.
     */
    val cameraHandler: HandlerThreadExecutor
        get() = _camera2Handler

    /**
     * Executor for camera operations.
     */
    val cameraExecutor: ExecutorService
        get() = _camera2Executor
}