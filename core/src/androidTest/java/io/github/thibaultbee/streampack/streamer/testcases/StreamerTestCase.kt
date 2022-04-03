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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.utils.AndroidUtils
import io.github.thibaultbee.streampack.utils.FakeAndroidLogger
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test

abstract class StreamerTestCase {
    protected val logger = FakeAndroidLogger()
    protected val context: Context = InstrumentationRegistry.getInstrumentation().context

    abstract val streamer: BaseStreamer

    @After
    open fun tearDown() {
        streamer.release()
    }

    @Test
    open fun defaultUsageTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startStream()
            streamer.stopStream()
            streamer.release()
        } catch (e: Exception) {
            fail("Default usage must not throw exception $e")
        }
    }

    // Single method calls
    @Test
    fun configureTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
        } catch (e: Exception) {
            fail("Must be possible to only configure without exception: $e")
        }
    }

    @Test
    fun configureErrorTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeInvalidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (e: Exception) {
        }
    }

    @Test
    fun startStreamTest() {
        try {
            streamer.startStream()
            fail("startStream without configuration must throw an exception")
        } catch (e: Exception) {
        }
    }

    @Test
    fun stopStreamTest() {
        try {
            streamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to only stopStream without exception: $e")
        }
    }

    @Test
    fun releaseTest() {
        try {
            streamer.release()
        } catch (e: Exception) {
            fail("Must be possible to only release without exception: $e")
        }
    }

    // Multiple methods calls
    @Test
    fun configureStartStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startStream()
            fail("startStream without startPreview must failed")
        } catch (e: Exception) {
        }
    }

    @Test
    fun configureReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.release()
        } catch (e: Exception) {
            fail("Must be possible to configure/release but catches exception: $e")
        }
    }

    @Test
    fun configureStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopStream but catches exception: $e")
        }
    }

    @Test
    open fun startStreamReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startStream()
            streamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startStream/release but catches exception: $e")
        }
    }

    @Test
    open fun startStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.startStream()
            streamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream but catches exception: $e")
        }
    }

    // Stress test
    @Test
    fun multipleConfigureTest() {
        try {
            (0..10).forEach { _ ->
                streamer.configure(
                    AndroidUtils.fakeValidAudioConfig(),
                    AndroidUtils.fakeValidVideoConfig()
                )
            }
        } catch (e: Exception) {
            fail("Must be possible to call configure multiple times but catches exception: $e")
        }
    }

    @Test
    open fun multipleStartStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            (0..10).forEach { _ ->
                streamer.startStream()
                streamer.stopStream()
            }
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream multiple times but catches exception: $e")
        }
    }
}