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
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.release()
    }

    // Single method calls
    @Test
    open fun configureAudioOnlyTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig()
        )
    }

    @Test
    open fun configureVideoOnlyTest() {
        streamer.configure(
            DataUtils.dummyValidVideoConfig()
        )
    }

    @Test
    open fun configureTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
    }

    @Test
    open fun configureErrorTest() {
        try {
            streamer.configure(
                DataUtils.dummyInvalidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun startStreamTest() = runTest {
        try {
            streamer.startStream()
            fail("startStream without descriptor")
        } catch (_: Throwable) {
        }

        try {
            streamer.startStream(descriptor)
            fail("startStream without configuration must throw an exception")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun stopStreamTest() = runTest {
        streamer.stopStream()
    }

    @Test
    fun releaseTest() {
        streamer.release()
    }

    // Multiple methods calls
    @Test
    open fun configureStartStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
    }

    @Test
    open fun configureReleaseTest() {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.release()
    }

    @Test
    open fun configureStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.stopStream()
    }

    @Test
    open fun startStreamReleaseTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.release()
    }

    @Test
    open fun startStreamStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
    }

    // Stress test
    @Test
    open fun multipleConfigureTest() {
        (0..10).forEach { _ ->
            streamer.configure(
                DataUtils.dummyValidAudioConfig(),
                DataUtils.dummyValidVideoConfig()
            )
        }
    }

    @Test
    open fun multipleStartStreamStopStreamTest() = runTest {
        streamer.configure(
            DataUtils.dummyValidAudioConfig(),
            DataUtils.dummyValidVideoConfig()
        )
        (0..10).forEach { _ ->
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.close()
        }
    }
}