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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSessionCallback
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestWithTargetsBuilder
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class CameraSessionController private constructor(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val captureSession: CameraCaptureSession,
    private val sessionCallback: CameraSessionCallback,
    private val sessionCompat: ICameraCaptureSessionCompat,
    val cameraIsClosedFlow: StateFlow<Boolean>,
    val isClosedFlow: StateFlow<Boolean>
) {
    private val captureSessionMutex = Mutex()

    val isClosed: Boolean
        get() = isClosedFlow.value || cameraIsClosedFlow.value

    /**
     * A default capture callback that logs the failure reason.
     */
    private val captureCallback = object : CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Logger.e(TAG, "Capture failed with code ${failure.reason}")
        }
    }

    private val captureCallbacks =
        setOf(captureCallback, sessionCallback)

    suspend fun close() {
        withContext(coroutineDispatcher) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Session already closed")
                    return@withContext
                }
                try {
                    captureSession.close()
                    combine(isClosedFlow, cameraIsClosedFlow) { isClosed, isClosedCamera ->
                        isClosed || isClosedCamera
                    }.first { it }
                } catch (t: Throwable) {
                    Logger.w(TAG, "Error closing camera session: $t")
                }
            }
        }
    }

    /**
     * Sets or stops a repeating session with the current capture request.
     *
     * If the [captureRequestBuilder] does not hold a [Surface], it will stop the repeating session.
     *
     * @param captureRequestBuilder The capture request builder to use
     */
    suspend fun applyRepeatingSession(captureRequestBuilder: CaptureRequestWithTargetsBuilder) {
        if (captureRequestBuilder.isEmpty()) {
            stopRepeatingSession()
        } else {
            setRepeatingSession(captureRequestBuilder)
        }
    }

    /**
     * Sets a repeating session with the current capture request.
     *
     * @param captureRequestBuilder The capture request builder to use
     */
    private suspend fun setRepeatingSession(captureRequestBuilder: CaptureRequestWithTargetsBuilder) {
        if (captureRequestBuilder.isEmpty()) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        withContext(coroutineDispatcher) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Camera session controller is released")
                    return@withContext
                }

                sessionCompat.setRepeatingSingleRequest(
                    captureSession,
                    captureRequestBuilder.build(),
                    MultiCaptureCallback(captureCallbacks)
                )
            }
        }
    }

    private suspend fun stopRepeatingSession() {
        withContext(coroutineDispatcher) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Camera session controller is released")
                    return@withContext
                }

                captureSession.stopRepeating()
            }
        }
    }

    /**
     * Creates a new capture session with the given outputs.
     *
     * The capture session of the current [CameraSessionController] will be closed.
     *
     * @param cameraDeviceController The [CameraDeviceController] to use.
     * @param outputs The outputs to use. By default it uses the current outputs.
     * @param dynamicRange The dynamic range to use
     * @return A new [CameraSessionController]
     */
    suspend fun recreate(
        cameraDeviceController: CameraDeviceController,
        outputs: List<CameraSurface>,
        dynamicRange: Long,
    ): CameraSessionController = withContext(coroutineDispatcher) {
        require(outputs.isNotEmpty()) { "At least one output is required" }
        require(outputs.all { it.surface.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.surface.isValid }} is invalid" }

        // Close current session
        close()

        val isClosedFlow = MutableStateFlow(false)
        val newCaptureSession =
            CameraUtils.createCaptureSession(
                cameraDeviceController,
                outputs.map { it.surface },
                dynamicRange,
                isClosedFlow
            )

        val controller = CameraSessionController(
            coroutineDispatcher,
            newCaptureSession,
            sessionCallback,
            sessionCompat,
            cameraDeviceController.isClosedFlow,
            isClosedFlow.asStateFlow()
        )

        controller
    }

    companion object {
        private const val TAG = "CameraSessionController"

        suspend fun create(
            cameraDeviceController: CameraDeviceController,
            sessionCallback: CameraSessionCallback,
            sessionCompat: ICameraCaptureSessionCompat,
            outputs: List<CameraSurface>,
            dynamicRange: Long,
            coroutineDispatcher: CoroutineDispatcher
        ): CameraSessionController {
            require(outputs.isNotEmpty()) { "At least one output is required" }
            require(outputs.all { it.surface.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.surface.isValid }} is invalid" }

            val isClosedFlow = MutableStateFlow(false)
            val captureSession =
                CameraUtils.createCaptureSession(
                    cameraDeviceController,
                    outputs.map { it.surface },
                    dynamicRange,
                    isClosedFlow
                )

            return CameraSessionController(
                coroutineDispatcher,
                captureSession,
                sessionCallback,
                sessionCompat,
                cameraDeviceController.isClosedFlow,
                isClosedFlow.asStateFlow()
            )
        }
    }

    private class MultiCaptureCallback(
        private val callbacks: Set<CaptureCallback>
    ) : CaptureCallback() {
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            callbacks.forEach {
                it.onCaptureStarted(session, request, timestamp, frameNumber)
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            callbacks.forEach {
                it.onCaptureCompleted(session, request, result)
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            callbacks.forEach {
                it.onCaptureFailed(session, request, failure)
            }
        }
    }
}
