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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraSessionController.CaptureResultListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A capture callback that wraps multiple [CaptureResultListener].
 *
 * @param coroutineScope The coroutine scope to use.
 */
internal class CameraSessionCallback(private val coroutineScope: CoroutineScope) :
    CaptureCallback() {
    /* synthetic accessor */
    private val resultListeners = mutableSetOf<CaptureResultListener>()

    /**
     * Adds a capture result listener.
     *
     * The listener is removed when [removeListener] is explicitly called or when [CaptureResultListener] returns true.
     */
    fun addListener(listener: CaptureResultListener) {
        resultListeners.add(listener)
    }

    fun removeListener(listener: CaptureResultListener) {
        resultListeners.remove(listener)
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        coroutineScope.launch {
            val removeSet = mutableSetOf<CaptureResultListener>()
            for (listener in resultListeners) {
                val isFinished: Boolean = listener.onCaptureResult(result)
                if (isFinished) {
                    removeSet.add(listener)
                }
            }
            if (!removeSet.isEmpty()) {
                resultListeners.removeAll(removeSet)
            }
        }
    }
}