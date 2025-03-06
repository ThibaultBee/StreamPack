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

import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource
import io.github.thibaultbee.streampack.core.streamer.single.utils.SingleStreamerConfigUtils
import io.github.thibaultbee.streampack.core.streamers.single.AudioOnlySingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AudioOnlySingleStreamerStateTest(private val descriptor: MediaDescriptor) :
    DeviceTest(withCamera = false) {
    private val streamer by lazy {
        runBlocking {
            AudioOnlySingleStreamer(context).apply { setAudioSource(MicrophoneSource.buildDefaultMicrophoneSource()) }
        }
    }

    private val audioConfig by lazy { SingleStreamerConfigUtils.audioConfig(descriptor) }

    @Test
    fun defaultUsageTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
        streamer.release()
    }

    // Single method calls
    @Test
    fun configureAudioOnlyTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
    }

    // Multiple methods calls
    @Test
    fun configureStartStreamTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.startStream(descriptor)
    }

    @Test
    fun configureReleaseTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.release()
    }

    @Test
    fun configureStopStreamTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.stopStream()
    }

    @Test
    fun startStreamReleaseTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.startStream(descriptor)
        streamer.release()
    }

    @Test
    fun startStreamStopStreamTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        streamer.startStream(descriptor)
        streamer.stopStream()
    }

    @Test
    fun multipleStartStreamStopStreamTest() = runTest {
        streamer.setAudioConfig(
            audioConfig
        )
        (0..10).forEach { _ ->
            streamer.startStream(descriptor)
            streamer.stopStream()
            streamer.close()
        }
    }

    @Test
    fun multipleConfigureTest() = runTest {
        (0..10).forEach { _ ->
            streamer.setAudioConfig(
                audioConfig
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "MediaDescriptor: {0}"
        )
        fun getMediaDescriptor(): Iterable<MediaDescriptor> {
            return arrayListOf(
                UriMediaDescriptor(FileUtils.createCacheFile("audio.ts").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("audio.mp4").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("audio.flv").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("audio.webm").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("audio.ogg").toUri()),
                UriMediaDescriptor(FileUtils.createCacheFile("audio.3gp").toUri())
            )
        }
    }
}