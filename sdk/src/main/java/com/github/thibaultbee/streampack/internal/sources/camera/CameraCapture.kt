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
package com.github.thibaultbee.streampack.internal.sources.camera

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.internal.interfaces.Streamable
import com.github.thibaultbee.streampack.utils.CameraSettings
import com.github.thibaultbee.streampack.utils.ILogger
import kotlinx.coroutines.runBlocking

class CameraCapture(
    context: Context,
    logger: ILogger
) : Streamable<Int> {
    var previewSurface: Surface? = null
    var encoderSurface: Surface? = null
    var cameraId: String = "0"
        get() = cameraController.camera?.id ?: field
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            runBlocking {
                val restartStream = isStreaming
                stopPreview()
                startPreview(value, restartStream)
            }
            field = value
        }
    private var cameraController = CameraController(context, logger = logger)
    var settings = CameraSettings(cameraController)

    /**
     * As timestamp source differs from one camera to another. Computes an offset.
     */
    val timestampOffset = CameraHelper.getTimeOffsetToMonoClock(context, cameraId)


    private var fps: Int = 30
    private var isStreaming = false
    internal var isPreviewing = false

    override fun configure(fps: Int) {
        this.fps = fps
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startPreview(cameraId: String = this.cameraId, restartStream: Boolean = false) {
        var targets = mutableListOf<Surface>()
        previewSurface?.let { targets.add(it) }
        encoderSurface?.let { targets.add(it) }
        cameraController.startCamera(cameraId, targets)

        targets = mutableListOf()
        previewSurface?.let { targets.add(it) }
        if (restartStream) {
            encoderSurface?.let { targets.add(it) }
        }
        cameraController.startRequestSession(fps, targets)
        isPreviewing = true
    }

    fun stopPreview() {
        isPreviewing = false
        cameraController.stopCamera()
    }

    private fun checkStream() =
        require(encoderSurface != null) { "encoder surface must not be null" }

    override fun startStream() {
        checkStream()

        cameraController.addTarget(encoderSurface!!)
        isStreaming = true
    }

    override fun stopStream() {
        if (isStreaming) {
            checkStream()

            isStreaming = false
            cameraController.removeTarget(encoderSurface!!)
        }
    }

    override fun release() {
        cameraController.release()
    }
}