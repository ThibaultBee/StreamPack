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

import android.hardware.camera2.TotalCaptureResult

interface CaptureResultListener {
    /**
     * Called when a capture result is received.
     *
     * @param result The capture result.
     * @return true if the listener is finished and should be removed, false otherwise.
     */
    fun onCaptureResult(result: TotalCaptureResult): Boolean
}