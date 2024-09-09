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
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.streamer.surface.SurfaceUtils
import io.github.thibaultbee.streampack.core.streamer.surface.SurfaceViewTestActivity
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.utils.ConfigurationUtils
import io.github.thibaultbee.streampack.core.utils.extensions.startStream
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

abstract class CameraStreamerTestCase :
    StreamerTestCase() {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context

    override val streamer = DefaultCameraStreamer(context)

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(SurfaceViewTestActivity::class.java)

    @Test
    fun defaultUsageTestWithPreview() = runTest {
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.createSurfaceView(activityScenarioRule.scenario))
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.stopPreview()
        streamer.release()
    }

    // Single method calls
    @Test
    open fun startPreviewTest() = runTest {
        streamer.startPreview(SurfaceUtils.createSurfaceView(activityScenarioRule.scenario))
    }

    @Test
    fun stopPreviewTest() {
        streamer.stopPreview()
    }

    // Multiple methods calls
    @Test
    fun configureStopPreviewTest() {
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.stopPreview()
    }

    @Test
    fun startPreviewReleaseTest() = runTest {
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.createSurfaceView(activityScenarioRule.scenario))
        streamer.release()
    }

    @Test
    fun startPreviewStopPreviewTest() = runTest {
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.createSurfaceView(activityScenarioRule.scenario))
        streamer.stopPreview()
    }

    @Test
    fun startPreviewStopStreamTest() = runTest {
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.createSurfaceView(activityScenarioRule.scenario))
        streamer.stopStream()
    }

    @Test
    fun multipleStartPreviewStopPreviewTest() = runTest {
        val surfaceView = SurfaceUtils.createSurfaceView(activityScenarioRule.scenario)
        streamer.configure(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        (0..10).forEach { _ ->
            streamer.startPreview(surfaceView)
            streamer.stopPreview()
        }
    }
}