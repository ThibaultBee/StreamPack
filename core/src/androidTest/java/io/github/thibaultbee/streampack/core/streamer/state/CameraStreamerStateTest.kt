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
package io.github.thibaultbee.streampack.core.streamer.state

import android.Manifest
import android.content.Context
import androidx.core.net.toUri
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamer.surface.SurfaceUtils
import io.github.thibaultbee.streampack.core.streamer.surface.SurfaceViewTestActivity
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import io.github.thibaultbee.streampack.core.streamers.single.CameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.setConfig
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import io.github.thibaultbee.streampack.core.utils.ConfigurationUtils
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CameraStreamerStateTest(descriptor: MediaDescriptor) :
    StreamerStateTest(
        descriptor
    ) {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    override val streamer = CameraSingleStreamer(context)

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(SurfaceViewTestActivity::class.java)

    @Test
    fun defaultUsageTestWithPreview() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.getSurfaceView(activityScenarioRule.scenario))
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.stopPreview()
        streamer.release()
    }

    // Single method calls
    @Test
    fun startPreviewTest() = runTest {
        streamer.startPreview(SurfaceUtils.getSurfaceView(activityScenarioRule.scenario))
    }

    @Test
    fun stopPreviewTest() = runTest {
        streamer.stopPreview()
    }

    // Multiple methods calls
    @Test
    fun configureStopPreviewTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.stopPreview()
    }

    @Test
    fun startPreviewReleaseTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.getSurfaceView(activityScenarioRule.scenario))
        streamer.release()
    }

    @Test
    fun startPreviewStopPreviewTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.getSurfaceView(activityScenarioRule.scenario))
        streamer.stopPreview()
    }

    @Test
    fun startPreviewStopStreamTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startPreview(SurfaceUtils.getSurfaceView(activityScenarioRule.scenario))
        streamer.stopStream()
    }

    @Test
    fun multipleStartPreviewStopPreviewTest() = runTest {
        val surfaceView = SurfaceUtils.getSurfaceView(activityScenarioRule.scenario)
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        (0..10).forEach { _ ->
            streamer.startPreview(surfaceView)
            streamer.stopPreview()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "MediaDescriptor: {0}"
        )
        fun getMediaDescriptor(): Iterable<MediaDescriptor> {
            return arrayListOf(
                UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("video.flv").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("video.webm").toUri())
            )
        }
    }
}