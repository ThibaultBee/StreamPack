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
import com.github.thibaultbee.streampack.streamers.BaseCaptureStreamer
import com.github.thibaultbee.streampack.utils.AndroidUtils
import com.github.thibaultbee.streampack.utils.FakeAndroidLogger
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BaseCaptureStreamerTest {
    private val logger = FakeAndroidLogger()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var captureStreamer: BaseCaptureStreamer
    private val surface = Surface(SurfaceTexture(true))

    @Before
    fun setUp() {
        captureStreamer = BaseCaptureStreamer(
            context,
            AndroidUtils.fakeServiceInfo(),
            FakeEndpoint(logger),
            logger
        )
    }

    @After
    fun clean() {
        captureStreamer.release()
    }

    @Test
    fun defaultUsageTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.startStream()
            captureStreamer.stopStream()
            captureStreamer.stopPreview()
            captureStreamer.release()
        } catch (e: Exception) {
            fail("Default usage must not throw exception ${e.message}")
        }
    }

    @Test
    fun startPreviewOnlyTest() {
        try {
            captureStreamer.startPreview(surface)
            fail("startPreview without configure must failed")
        } catch (e: Exception) {
        }
    }

    @Test
    fun startStreamOnlyTest() {
        try {
            captureStreamer.startStream()
            fail("startStream without configure nor startPreview must failed")
        } catch (e: Exception) {
        }

        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startStream()
            fail("startStream without startPreview must failed")
        } catch (e: Exception) {
        }
    }

    @Test
    fun releaseTest() {
        try {
            captureStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to only release without exception: ${e.message}")
        }
    }

    @Test
    fun stopPreviewTest() {
        try {
            captureStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to only stopPreview without exception: ${e.message}")
        }
    }

    @Test
    fun stopStreamTest() {
        try {
            captureStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to only stopStream without exception: ${e.message}")
        }
    }

    @Test
    fun configureReleaseTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to configure/release but catches exception: ${e.message}")
        }
    }

    @Test
    fun configureStopPreviewTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun configureStopStreamTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopStream but catches exception: ${e.message}")
        }
    }

    @Test
    fun onConfigurationErrorTest() {
        try {
            captureStreamer.configure(
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
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/release but catches exception: ${e.message}")
        }
    }


    @Test
    fun startPreviewStopPreviewTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun startPreviewStopStreamTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startPreview/stopStream but catches exception: ${e.message}")
        }
    }


    @Test
    fun startStreamReleaseTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.startStream()
            captureStreamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startStream/release but catches exception: ${e.message}")
        }
    }


    @Test
    fun startStreamStopPreviewTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.startStream()
            captureStreamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopPreview but catches exception: ${e.message}")
        }
    }

    @Test
    fun startStreamStopStreamTest() {
        try {
            captureStreamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            captureStreamer.startPreview(surface)
            captureStreamer.startStream()
            captureStreamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream but catches exception: ${e.message}")
        }
    }
}