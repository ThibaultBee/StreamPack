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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.DefaultAudioOnlyStreamer
import io.github.thibaultbee.streampack.core.utils.DataUtils
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
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.release()
    }

    // Single method calls
    @Test
    fun configureAudioOnlyTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
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
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.startStream(descriptor)
    }

    @Test
    fun configureReleaseTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.release()
    }

    @Test
    fun configureStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.stopStream()
    }

    @Test
    fun startStreamReleaseTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.startStream(descriptor)
        streamer.release()
    }

    @Test
    fun startStreamStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
    }

    @Test
    fun multipleStartStreamStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
        (0..10).forEach { _ ->
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.close()
        }
    }

    @Test
    fun multipleConfigureTest() {
        (0..10).forEach { _ ->
            streamer.configure(
                DataUtils.dummyValidAudioConfig()
            )
        }
    }
}