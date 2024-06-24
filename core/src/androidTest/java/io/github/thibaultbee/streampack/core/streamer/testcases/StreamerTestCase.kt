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

import android.util.Log
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.DefaultStreamer
import io.github.thibaultbee.streampack.core.utils.DataUtils
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test

abstract class StreamerTestCase {
    protected abstract val streamer: DefaultStreamer
    protected abstract val descriptor: MediaDescriptor

    @After
    open fun tearDown() {
        streamer.release()
    }

    @Test
    open fun defaultUsageTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "defaultUsageTest: exception: ", e)
            fail("Default usage must not throw exception $e")
        }
    }

    // Single method calls
    @Test
    open fun configureAudioOnlyTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
        } catch (e: Exception) {
            Log.e(TAG, "configureAudioTest: exception: ", e)
            fail("Must be possible to only configure audio without exception: $e")
        }
    }

    @Test
    open fun configureVideoOnlyTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidVideoConfig()
            )
        } catch (e: Exception) {
            Log.e(TAG, "configureVideoTest: exception: ", e)
            fail("Must be possible to only configure video without exception: $e")
        }
    }

    @Test
    open fun configureTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
        } catch (e: Exception) {
            Log.e(TAG, "configureTest: exception: ", e)
            fail("Must be possible to only configure without exception: $e")
        }
    }

    @Test
    open fun configureErrorTest() {
        try {
            streamer.configure(
                DataUtils.dummyInvalidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun startStreamTest() = runTest {
        try {
            streamer.startStream()
            fail("startStream without configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun stopStreamTest() = runTest {
        try {
            streamer.stopStream()
        } catch (e: Exception) {
            Log.e(TAG, "stopStreamTest: exception: ", e)
            fail("Must be possible to only stopStream without exception: $e")
        }
    }

    @Test
    fun releaseTest() {
        try {
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "releaseTest: exception: ", e)
            fail("Must be possible to only release without exception: $e")
        }
    }

    // Multiple methods calls
    @Test
    open fun configureStartStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.startStream()
            fail("startStream without startPreview must failed")
        } catch (_: Exception) {
        }
    }

    @Test
    open fun configureReleaseTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "configureReleaseTest: exception: ", e)
            fail("Must be possible to configure/release but catches exception: $e")
        }
    }

    @Test
    open fun configureStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.stopStream()
        } catch (e: Exception) {
            Log.e(TAG, "configureStopStreamTest: exception: ", e)
            fail("Must be possible to configure/stopStream but catches exception: $e")
        }
    }

    @Test
    open fun startStreamReleaseTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.startStream()
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamReleaseTest: exception: ", e)
            fail("Must be possible to startStream/release but catches exception: $e")
        }
    }

    @Test
    open fun startStreamStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            streamer.startStream()
            streamer.stopStream()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream but catches exception: $e")
        }
    }

    // Stress test
    @Test
    open fun multipleConfigureTest() {
        try {
            (0..10).forEach { _ ->
                streamer.configure(
                    DataUtils.dummyValidAudioConfig(),
                    DataUtils.dummyValidVideoConfig()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleConfigureTest: exception: ", e)
            fail("Must be possible to call configure multiple times but catches exception: $e")
        }
    }

    @Test
    open fun multipleStartStreamStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            (0..10).forEach { _ ->
                streamer.startStream()
                streamer.stopStream()
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleStartStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream multiple times but catches exception: $e")
        }
    }

    companion object {
        private const val TAG = "StreamerTestCase"
    }
}