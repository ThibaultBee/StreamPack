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
package com.github.thibaultbee.streampack.streamer

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.github.thibaultbee.streampack.internal.endpoints.FakeEndpoint
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer
import com.github.thibaultbee.streampack.utils.AndroidUtils
import com.github.thibaultbee.streampack.utils.FakeAndroidLogger
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BaseCameraStreamerTest {
    private val logger = FakeAndroidLogger()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var cameraStreamer: BaseCameraStreamer
    private val surface = Surface(SurfaceTexture(true))

    @Before
    fun setUp() {
        cameraStreamer = BaseCameraStreamer(
            context,
            AndroidUtils.fakeServiceInfo(),
            FakeEndpoint(logger),
            logger
        )
    }

    @After
    fun clean() {
        cameraStreamer.release()
    }

    @Test
    fun defaultUsageTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.startStream()
            cameraStreamer.stopStream()
            cameraStreamer.stopPreview()
            cameraStreamer.release()
        } catch (e: Exception) {
            fail("Default usage must not throw exception ${e.message}")
        }
    }

    @Test
    fun startPreviewOnlyTest() {
        try {
            cameraStreamer.startPreview(surface)
            fail("startPreview without configure must failed")
        } catch (e: Exception) {
        }
    }

    @Test
    fun startStreamOnlyTest() {
        try {
            cameraStreamer.startStream()
            fail("startStream without configure nor startPreview must failed")
        } catch (e: Exception) {
        }

        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startStream()
            fail("startStream without startPreview must failed")
        } catch (e: Exception) {
        }
    }

    @Test
    fun releaseTest() {
        try {
            cameraStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to only release without exception: ${e.message}")
        }
    }

    @Test
    fun stopPreviewTest() {
        try {
            cameraStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to only stopPreview without exception: ${e.message}")
        }
    }

    @Test
    fun stopStreamTest() {
        try {
            cameraStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to only stopStream without exception: ${e.message}")
        }
    }

    @Test
    fun configureReleaseTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to configure/release but catches exception: ${e.message}")
        }
    }

    @Test
    fun configureStopPreviewTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun configureStopStreamTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopStream but catches exception: ${e.message}")
        }
    }

    @Test
    fun onConfigurationErrorTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeInvalidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (e: Exception) {
        }
    }

    @Test
    fun startPreviewReleaseTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/release but catches exception: ${e.message}")
        }
    }


    @Test
    fun startPreviewStopPreviewTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun startPreviewStopStreamTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/stopStream but catches exception: ${e.message}")
        }
    }


    @Test
    fun startStreamReleaseTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.startStream()
            cameraStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startStream/release but catches exception: ${e.message}")
        }
    }


    @Test
    fun startStreamStopPreviewTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.startStream()
            cameraStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun startStreamStopStreamTest() {
        try {
            cameraStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            cameraStreamer.startPreview(surface)
            cameraStreamer.startStream()
            cameraStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream but catches exception: ${e.message}")
        }
    }
}