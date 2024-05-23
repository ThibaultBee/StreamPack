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
import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.streamers.DefaultAudioOnlyStreamer
import io.github.thibaultbee.streampack.utils.DataUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

abstract class AudioOnlyStreamerTestCase {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private val streamer = DefaultAudioOnlyStreamer(context)

    protected abstract val descriptor: MediaDescriptor

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun defaultUsageTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
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
    fun configureAudioOnlyTest() {
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
    fun configureVideoOnlyTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidVideoConfig()
            )
            fail("Must not be possible to configure video")
        } catch (_: Exception) {
        }
    }

    @Test
    fun configureTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            fail("Must not be possible to configure video")
        } catch (_: Exception) {
        }
    }

    @Test
    fun configureErrorTest() {
        try {
            streamer.configure(
                DataUtils.dummyInvalidAudioConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (_: Exception) {
        }
    }

    // Multiple methods calls
    @Test
    fun configureStartStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            streamer.startStream(descriptor)
        } catch (e: Exception) {
            Log.e(TAG, "configureStartStreamTest: exception: ", e)
            fail("Must be possible to configure/startStream but catches exception: $e")
        }
    }

    @Test
    fun configureReleaseTest() {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "configureReleaseTest: exception: ", e)
            fail("Must be possible to configure/release but catches exception: $e")
        }
    }

    @Test
    fun configureStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            streamer.stopStream()
        } catch (e: Exception) {
            Log.e(TAG, "configureStopStreamTest: exception: ", e)
            fail("Must be possible to configure/stopStream but catches exception: $e")
        }
    }

    @Test
    fun startStreamReleaseTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            streamer.startStream(descriptor)
            streamer.release()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamReleaseTest: exception: ", e)
            fail("Must be possible to startStream/release but catches exception: $e")
        }
    }

    @Test
    fun startStreamStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            streamer.startStream(descriptor)
            streamer.stopStream()
        } catch (e: Exception) {
            Log.e(TAG, "startStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream but catches exception: $e")
        }
    }

    @Test
    fun multipleStartStreamStopStreamTest() = runTest {
        try {
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
            (0..10).forEach { _ ->
                streamer.startStream(descriptor)
                streamer.stopStream()
                streamer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleStartStreamStopStreamTest: exception: ", e)
            fail("Must be possible to startStream/stopStream multiple times but catches exception: $e")
        }
    }

    @Test
    fun multipleConfigureTest() {
        try {
            (0..10).forEach { _ ->
                streamer.configure(
                    DataUtils.dummyValidAudioConfig()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "multipleConfigureTest: exception: ", e)
            fail("Must be possible to call configure multiple times but catches exception: $e")
        }
    }

    companion object {
        private const val TAG = "AudioOnlyStreamerTestCase"
    }
}