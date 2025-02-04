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

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import io.github.thibaultbee.streampack.core.utils.ConfigurationUtils
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test

abstract class StreamerStateTest(
    protected val descriptor: MediaDescriptor
) : DeviceTest() {
    protected abstract val streamer: SingleStreamer

    @After
    open fun tearDown() {
        streamer.releaseBlocking()
    }

    @Test
    open fun defaultUsageTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.release()
    }

    // Single method calls
    @Test
    open fun configureAudioOnlyTest() = runTest {
        streamer.setAudioConfig(
            ConfigurationUtils.dummyValidAudioConfig()
        )
    }

    @Test
    open fun configureVideoOnlyTest() = runTest {
        streamer.setVideoConfig(
            ConfigurationUtils.dummyValidVideoConfig()
        )
    }

    @Test
    open fun configureTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
    }

    @Test
    open fun configureErrorTest() = runTest {
        try {
            streamer.setConfig(
                ConfigurationUtils.dummyInvalidAudioConfig(),
                ConfigurationUtils.dummyValidVideoConfig()
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
    fun releaseTest() = runTest {
        streamer.release()
    }

    // Multiple methods calls
    @Test
    open fun configureStartStreamTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
    }

    @Test
    open fun configureReleaseTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.release()
    }

    @Test
    open fun configureStopStreamTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.stopStream()
    }

    @Test
    open fun startStreamReleaseTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.release()
    }

    @Test
    open fun startStreamStopStreamTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
    }

    // Stress test
    @Test
    open fun multipleConfigureTest() = runTest {
        (0..10).forEach { _ ->
            streamer.setConfig(
                ConfigurationUtils.dummyValidAudioConfig(),
                ConfigurationUtils.dummyValidVideoConfig()
            )
        }
    }

    @Test
    open fun multipleStartStreamStopStreamTest() = runTest {
        streamer.setConfig(
            ConfigurationUtils.dummyValidAudioConfig(),
            ConfigurationUtils.dummyValidVideoConfig()
        )
        (0..10).forEach { _ ->
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.close()
        }
    }
}