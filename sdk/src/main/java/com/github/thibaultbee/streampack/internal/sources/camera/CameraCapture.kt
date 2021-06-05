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
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.internal.interfaces.Streamable
import com.github.thibaultbee.streampack.utils.ILogger
import com.github.thibaultbee.streampack.utils.getCameraFpsList
import kotlinx.coroutines.runBlocking
import java.security.InvalidParameterException

class CameraCapture(
    private val context: Context,
    private val logger: ILogger
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

    private var fpsRange = Range(0, 30)
    private var isStreaming = false
    internal var isPreviewing = false

    private var cameraController = CameraController(context, logger = logger)

    private fun getClosestFpsRange(fps: Int): Range<Int> {
        var fpsRangeList = context.getCameraFpsList(cameraId)
        logger.d(this, "$fpsRangeList")

        // Get range that contains FPS
        fpsRangeList =
            fpsRangeList.filter { it.contains(fps) or it.contains(fps * 1000) } // On Samsung S4 fps range is [4000-30000] instead of [4-30]
        if (fpsRangeList.isEmpty()) {
            throw InvalidParameterException("Failed to find a single FPS range that contains $fps")
        }

        // Get smaller range
        var selectedFpsRange = fpsRangeList[0]
        fpsRangeList = fpsRangeList.drop(0)
        fpsRangeList.forEach {
            if ((it.upper - it.lower) < (selectedFpsRange.upper - selectedFpsRange.lower)) {
                selectedFpsRange = it
            }
        }

        logger.d(this, "Selected Fps range $selectedFpsRange")
        return selectedFpsRange
    }

    override fun configure(fps: Int) {
        fpsRange = getClosestFpsRange(fps)
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
        cameraController.startRequestSession(fpsRange, targets)
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