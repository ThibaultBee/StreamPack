/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer.dual.file

import android.util.Size
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.test.filters.LargeTest
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.Mp4Muxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource
import io.github.thibaultbee.streampack.core.streamer.dual.utils.DualStreamerConfigUtils
import io.github.thibaultbee.streampack.core.streamer.utils.StreamerUtils
import io.github.thibaultbee.streampack.core.streamer.utils.VideoUtils
import io.github.thibaultbee.streampack.core.streamers.dual.CameraDualStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds

@LargeTest
@RunWith(Parameterized::class)
class CameraDualStreamerFileTest(
    private val firstDescriptor: MediaDescriptor,
    private val verifyFirst: Boolean,
    firstEndpointFactory: IEndpointInternal.Factory?,
    private val secondDescriptor: MediaDescriptor,
    private val verifySecond: Boolean,
    secondEndpointFactory: IEndpointInternal.Factory?,
) : DeviceTest() {
    private val streamer by lazy {
        runBlocking {
            if (firstEndpointFactory != null && secondEndpointFactory != null) {
                CameraDualStreamer(
                    context,
                    firstEndpointInternalFactory = firstEndpointFactory,
                    secondEndpointInternalFactory = secondEndpointFactory
                )
            } else if (firstEndpointFactory != null) {
                CameraDualStreamer(context, firstEndpointInternalFactory = firstEndpointFactory)
            } else if (secondEndpointFactory != null) {
                CameraDualStreamer(context, secondEndpointInternalFactory = secondEndpointFactory)
            } else {
                CameraDualStreamer(context)
            }
        }
    }

    private val audioConfig by lazy {
        DualStreamerConfigUtils.audioConfig(firstDescriptor, secondDescriptor)
    }
    private val videoConfig by lazy {
        DualStreamerConfigUtils.videoConfig(
            firstDescriptor, secondDescriptor, Size(
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            )
        )
    }

    @After
    fun tearDown() {
        streamer.releaseBlocking()
        // Delete files
        firstDescriptor.uri.toFile().delete()
        secondDescriptor.uri.toFile().delete()
    }

    @Test
    fun writeToFile() = runTest(timeout = TEST_TIMEOUT_MS.milliseconds) {
        // Configure
        streamer.setConfig(
            audioConfig, videoConfig
        )

        // Run stream
        StreamerUtils.runDualStream(
            streamer,
            firstDescriptor,
            secondDescriptor,
            STREAM_DURATION_MS.milliseconds,
            STREAM_POLLING_MS.milliseconds
        )
        streamer.release()

        // Verify
        VideoUtils.verifyFile(
            context,
            firstDescriptor.uri,
            verifyFirst,
            audioConfig.firstAudioConfig,
            videoConfig.firstVideoConfig
        )
        VideoUtils.verifyFile(
            context,
            secondDescriptor.uri,
            verifySecond,
            audioConfig.secondAudioConfig,
            videoConfig.secondVideoConfig
        )
    }

    companion object {
        private const val TEST_TIMEOUT_MS = 60_000L
        private const val STREAM_DURATION_MS = 30_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720

        @JvmStatic
        @Parameterized.Parameters(
            name = "First mediaDescriptor: {0} - first verify: {1} - first endpoint: {2} - second mediaDescriptor: {3} - second verify: {4} - second endpoint: {5}"
        )
        fun getMediaDescriptor(): Iterable<Array<Any?>> {
            return arrayListOf(
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()),
                    true,
                    null,
                    UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
                    true,
                    null
                ),
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
                    true,
                    null,
                    UriMediaDescriptor(FileUtils.createCacheFile("video.flv").toUri()),
                    false,
                    null
                ),
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.webm").toUri()),
                    true,
                    null,
                    UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
                    true,
                    CompositeEndpointFactory(Mp4Muxer(), FileSink())
                ),
            )
        }
    }
}