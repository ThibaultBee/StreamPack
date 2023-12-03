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
package io.github.thibaultbee.streampack.streamer.testcases

import android.Manifest
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.utils.AndroidUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class CameraStreamerTestCase :
    StreamerTestCase() {
    abstract override val streamer: BaseCameraStreamer
    private lateinit var surface: Surface

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
    override fun defaultUsageTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.startStream()
                streamer.stopStream()
            }
            streamer.stopPreview()
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "defaultUsageTest: exception: ", e)
            fail("Default usage must not throw exception $e")
        }
    }

    @Test
    override fun defaultUsageTest2() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.configure(
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.startStream()
                streamer.stopStream()
            }
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "defaultUsageTest2: exception: ", e)
            fail("Default usage must not throw exception $e")
        }
    }

    // Single method calls
    @Test
    fun startPreviewTest() {
        try {
            streamer.startPreview(surface)
            fail("startPreviewTest without configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun stopPreviewTest() {
        try {
            streamer.stopPreview()
        } catch (e: Exception) {
            fail("Must be possible to only stopPreview without exception: $e")
        }
    }

    // Multiple methods calls
    @Test
    fun configureStopPreviewTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.stopPreview()
        } catch (e: Exception) {
            Log.e(TAG, "configureStopPreviewTest: exception: ", e)
            fail("Must be possible to configure/stopPreview but catches exception: $e")
        }
    }

    @Test
    fun startPreviewReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "startPreviewReleaseTest: exception: ", e)
            fail("Must be possible to startPreview/release but catches exception: $e")
        }
    }

    @Test
    fun startPreviewStopPreviewTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            streamer.stopPreview()
        } catch (e: Exception) {
            Log.e(TAG, "startPreviewStopPreviewTest: exception: ", e)
            fail("Must be possible to startPreview/stopPreview but catches exception: $e")
        }
    }

    @Test
    fun startPreviewStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.stopStream()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startPreviewStopStreamTest: exception: ", e)
            fail("Must be possible to startPreview/stopStream but catches exception: $e")
        }
    }


    @Test
    override fun startStreamReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.startStream()
            }
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamReleaseTest: exception: ", e)
            fail("Must be possible to startStream/release but catches exception: $e")
        }
    }


    @Test
    fun startStreamStopPreviewTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.startStream()
            }
            streamer.stopPreview()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamStopPreviewTest: exception: ", e)
            fail("Must be possible to startStream/stopPreview but catches exception: $e")
        }
    }

    @Test
    override fun startStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            runBlocking {
                streamer.startStream()
                streamer.stopStream()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream but catches exception: $e")
        }
    }

    @Test
    fun multipleStartPreviewStopPreviewTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            (0..10).forEach { _ ->
                streamer.startPreview(surface)
                streamer.stopPreview()
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleStartPreviewStopPreviewTest: exception: ", e)
            fail("Must be possible to startPreview/stopPreview multiple times but catches exception: $e")
        }
    }

    @Test
    override fun multipleStartStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startPreview(surface)
            (0..10).forEach { _ ->
                runBlocking {
                    streamer.startStream()
                    streamer.stopStream()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleStartStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream multiple times but catches exception: $e")
        }
    }

    companion object {
        private const val TAG = "CameraStreamerTestCase"
    }
}