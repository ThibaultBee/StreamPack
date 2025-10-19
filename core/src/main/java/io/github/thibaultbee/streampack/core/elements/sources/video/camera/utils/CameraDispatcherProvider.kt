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