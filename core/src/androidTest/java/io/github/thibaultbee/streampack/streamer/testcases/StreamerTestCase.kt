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
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.utils.AndroidUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test

abstract class StreamerTestCase {
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
            runBlocking {
                streamer.startStream()
                streamer.stopStream()
            }
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "defaultUsageTest: exception: ", e)
            fail("Default usage must not throw exception $e")
        }
    }

    @Test
    open fun defaultUsageTest2() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.configure(
                AndroidUtils.fakeValidVideoConfig()
            )
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
    open fun configureAudioTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
        } catch (e: Exception) {
            Log.e(TAG, "configureAudioTest: exception: ", e)
            fail("Must be possible to only configure audio without exception: $e")
        }
    }

    @Test
    open fun configureVideoTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidVideoConfig()
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
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
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
                AndroidUtils.fakeInvalidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun startStreamTest() {
        try {
            runBlocking {
                streamer.startStream()
            }
            fail("startStream without configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun stopStreamTest() {
        try {
            runBlocking {
                streamer.stopStream()
            }
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
    open fun configureStartStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            runBlocking {
                streamer.startStream()
            }
            fail("startStream without startPreview must failed")
        } catch (_: Exception) {
        }
    }

    @Test
    open fun configureReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "configureReleaseTest: exception: ", e)
            fail("Must be possible to configure/release but catches exception: $e")
        }
    }

    @Test
    open fun configureStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            runBlocking {
                streamer.stopStream()
            }
        } catch (e: Exception) {
            Log.e(TAG, "configureStopStreamTest: exception: ", e)
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
    open fun startStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            runBlocking {
                streamer.startStream()
                streamer.stopStream()
            }
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
                    AndroidUtils.fakeValidAudioConfig(),
                    AndroidUtils.fakeValidVideoConfig()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleConfigureTest: exception: ", e)
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
        private const val TAG = "StreamerTestCase"
    }
}