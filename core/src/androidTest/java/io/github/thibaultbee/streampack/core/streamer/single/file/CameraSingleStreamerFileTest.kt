/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer.single.file

import android.util.Log
import android.util.Size
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.test.filters.LargeTest
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.streamer.single.utils.SingleStreamerConfigUtils
import io.github.thibaultbee.streampack.core.streamer.utils.StreamerUtils
import io.github.thibaultbee.streampack.core.streamer.utils.VideoUtils
import io.github.thibaultbee.streampack.core.streamers.single.cameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.setConfig
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
class CameraSingleStreamerFileTest(
    private val descriptor: MediaDescriptor,
    private val verify: Boolean,
    endpointFactory: IEndpointInternal.Factory?
) : DeviceTest() {
    private val streamer by lazy {
        runBlocking {
            if (endpointFactory != null) {
                cameraSingleStreamer(context, endpointFactory = endpointFactory)
            } else {
                cameraSingleStreamer(context)
            }
        }
    }

    private val audioConfig by lazy {
        SingleStreamerConfigUtils.audioConfig(descriptor)
    }
    private val videoConfig by lazy {
        SingleStreamerConfigUtils.videoConfig(descriptor, Size(VIDEO_WIDTH, VIDEO_HEIGHT))
    }

    @After
    fun tearDown() {
        try {
            Log.e(TAG, "Release")
            streamer.releaseBlocking()
        } catch (t: Throwable) {
            Log.e(TAG, "Release failed with $t", t)
        }
        // Delete file
        descriptor.uri.toFile().delete()
    }

    @Test
    fun writeToFile() = runTest(timeout = TEST_TIMEOUT_MS.milliseconds) {
        // Run stream
        streamer.setConfig(
            audioConfig,
            videoConfig
        )

        // Run stream
        StreamerUtils.runSingleStream(
            streamer,
            descriptor,
            STREAM_DURATION_MS.milliseconds,
            STREAM_POLLING_MS.milliseconds
        )
        streamer.release()

        // Check file
        val uri = descriptor.uri
        VideoUtils.verifyFile(uri.toFile())

        // Check video metadata
        if (verify) {
            VideoUtils.verify(context, uri, audioConfig, videoConfig)
        }
    }

    companion object {
        private const val TAG = "CameraSinStrFileTest"

        private const val TEST_TIMEOUT_MS = 60_000L
        private const val STREAM_DURATION_MS = 30_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720

        @JvmStatic
        @Parameterized.Parameters(
            name = "MediaDescriptor: {0} - Verify: {1} - Endpoint: {2}"
        )
        fun getMediaDescriptor(): Iterable<Array<Any?>> {
            return arrayListOf(
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()),
                    true,
                    null
                ),
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
                    true,
                    null
                ),
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.flv").toUri()),
                    false,
                    null
                ),
                arrayOf(
                    UriMediaDescriptor(FileUtils.createCacheFile("video.webm").toUri()),
                    true,
                    null
                )
            )
        }
    }
}