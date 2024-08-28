/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer.testcases

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.utils.DataUtils
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class CameraStreamerTestCase :
    StreamerTestCase() {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context

    override val streamer = DefaultCameraStreamer(context)
    protected lateinit var surface: Surface

    @Before
    fun setUp() {
        surface = Surface(SurfaceTexture(false))
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @Test
    fun defaultUsageTestWithPreview() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(surface)
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.stopPreview()
        streamer.release()
    }

    // Single method calls
    @Test
    open fun startPreviewTest() = runTest {
        streamer.startPreview(surface)
    }

    @Test
    fun stopPreviewTest() {
        streamer.stopPreview()
    }

    // Multiple methods calls
    @Test
    fun configureStopPreviewTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.stopPreview()
    }

    @Test
    fun startPreviewReleaseTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(surface)
        streamer.release()
    }

    @Test
    fun startPreviewStopPreviewTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(surface)
        streamer.stopPreview()
    }

    @Test
    fun startPreviewStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(surface)
        streamer.stopStream()
    }

    @Test
    fun startStreamStopPreviewTest() = runTest {

        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(surface)
        streamer.startStream(descriptor)
        streamer.stopPreview()
    }

    @Test
    fun multipleStartPreviewStopPreviewTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        (0..10).forEach { _ ->
            streamer.startPreview(surface)
            streamer.stopPreview()
        }
    }
}