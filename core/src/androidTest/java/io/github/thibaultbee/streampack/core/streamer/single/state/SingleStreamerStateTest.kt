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
package io.github.thibaultbee.streampack.core.streamer.single.state

import android.util.Log
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.streamers.single.setConfig
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test

abstract class SingleStreamerStateTest(
    protected val descriptor: MediaDescriptor
) : DeviceTest() {
    protected abstract val streamer: SingleStreamer

    protected abstract val audioConfig: AudioConfig
    protected abstract val videoConfig: VideoConfig

    @After
    fun tearDown() {
        try {
            Log.e(TAG, "Release")
            streamer.releaseBlocking()
        } catch (t: Throwable) {
            Log.e(TAG, "Release failed with $t", t)
        }
    }

    @Test
    open fun defaultUsageTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.release()
    }

    // Single method calls
    @Test
    open fun configureAudioOnlyTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
    }

    @Test
    open fun configureVideoOnlyTest() = runTest {
        streamer.setVideoConfig(
            videoConfig
        )
    }

    @Test
    open fun configureTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
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
            audioConfig, videoConfig
        )
        streamer.startStream(descriptor)
    }

    @Test
    open fun configureReleaseTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        streamer.release()
    }

    @Test
    open fun configureStopStreamTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        streamer.stopStream()
    }

    @Test
    open fun startStreamReleaseTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        streamer.startStream(descriptor)
        streamer.release()
    }

    @Test
    open fun startStreamStopStreamTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
    }

    // Stress test
    @Test
    open fun multipleConfigureTest() = runTest {
        (0..10).forEach { _ ->
            streamer.setConfig(
                audioConfig, videoConfig
            )
        }
    }

    @Test
    open fun multipleStartStreamStopStreamTest() = runTest {
        streamer.setConfig(
            audioConfig, videoConfig
        )
        (0..10).forEach { _ ->
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.close()
        }
    }

    companion object {
        const val TAG = "SingleStreamerStateTest"
    }
}