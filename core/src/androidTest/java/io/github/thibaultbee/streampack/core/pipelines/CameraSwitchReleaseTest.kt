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
package io.github.thibaultbee.streampack.core.pipelines

import android.Manifest
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(InternalStreamPackApi::class)
class CameraSwitchReleaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun testReleaseAfterACameraSwitchStopsItsHandlerThreads() = runTest {
        val cameraIds = context.getSystemService(CameraManager::class.java).cameraIdList
        assumeTrue("Needs two cameras", cameraIds.size >= 2)

        val dispatcherProvider = HandlerRecordingDispatcherProvider()
        val streamer = SingleStreamer(context, dispatcherProvider = dispatcherProvider)
        streamer.setVideoSource(CameraSourceFactory(cameraIds[0]))
        streamer.setVideoConfig(VideoConfig(resolution = Size(640, 480)))
        streamer.setVideoSource(CameraSourceFactory(cameraIds[1]))
        streamer.release()

        // Only surface processors' GL threads: the camera uses an executor from API 28.
        assertTrue(dispatcherProvider.handlerExecutors.isNotEmpty())
        // The surface processor's GL thread quits only once its last input surface is removed.
        dispatcherProvider.handlerExecutors.forEach {
            val thread = it.handler.looper.thread
            thread.join(5_000)
            assertFalse("Thread still running: ${thread.name}", thread.isAlive)
        }
    }

    /** Keeps the handler threads it creates. */
    private class HandlerRecordingDispatcherProvider(
        private val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    ) : IDispatcherProvider by dispatcherProvider {
        val handlerExecutors = CopyOnWriteArrayList<HandlerThreadExecutor>()

        override fun createVideoHandlerExecutor(componentName: String) =
            dispatcherProvider.createVideoHandlerExecutor(componentName)
                .also { handlerExecutors.add(it) }
    }
}
