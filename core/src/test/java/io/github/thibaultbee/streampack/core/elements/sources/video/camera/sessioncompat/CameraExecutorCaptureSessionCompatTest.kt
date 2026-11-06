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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.elements.utils.StubLogger
import io.github.thibaultbee.streampack.core.logger.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraExecutorCaptureSessionCompatTest {
    init {
        Logger.logger = StubLogger()
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val sessionCompat = CameraExecutorCaptureSessionCompat(
        CameraDispatcherProvider(Dispatchers.Default, { mockk() }, { cameraExecutor })
    )

    @Test
    fun `release shuts down the camera executor`() {
        sessionCompat.release()

        assertTrue(cameraExecutor.isShutdown)
    }

    @Test
    fun `callback after release is dropped`() {
        val manager = mockk<CameraManager>()
        val executor = slot<Executor>()
        every {
            manager.openCamera(any<String>(), capture(executor), any<CameraDevice.StateCallback>())
        } just Runs
        sessionCompat.openCamera(manager, "0", mockk())

        sessionCompat.release()

        executor.captured.execute { fail("Callback must not run after release") }
    }
}
