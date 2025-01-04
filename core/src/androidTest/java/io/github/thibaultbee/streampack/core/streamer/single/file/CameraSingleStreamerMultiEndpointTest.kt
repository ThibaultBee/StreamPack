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
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamer.utils.StreamerUtils
import io.github.thibaultbee.streampack.core.streamer.utils.VideoUtils
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.CameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Test [CameraSingleStreamer] with multiple endpoint.
 */
@LargeTest
class CameraSingleStreamerMultiEndpointTest : DeviceTest() {
    private val streamer by lazy { CameraSingleStreamer(context) }

    private val descriptors = listOf(
        UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()),
        UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()),
        UriMediaDescriptor(FileUtils.createCacheFile("video2.ts").toUri()),
        UriMediaDescriptor(FileUtils.createCacheFile("video2.mp4").toUri()),
    )

    @Test
    fun writeToEndpoints() = runTest(timeout = TEST_TIMEOUT_MS.milliseconds * descriptors.size) {
        val audioConfig = AudioConfig()
        val videoConfig = VideoConfig(resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT))

        // Run stream
        streamer.setConfig(
            audioConfig,
            videoConfig
        )

        descriptors.forEachIndexed { index, descriptor ->
            // Run amd verify first stream
            runStreamAndVerify(descriptor, audioConfig, videoConfig)
            Log.i(TAG, "Stream $index done: $descriptor")
        }

        streamer.release()
    }

    private suspend fun runStreamAndVerify(
        descriptor: UriMediaDescriptor,
        audioConfig: AudioConfig,
        videoConfig: VideoConfig
    ) {
        // Run stream
        StreamerUtils.runSingleStream(
            streamer,
            descriptor,
            STREAM_DURATION_MS.milliseconds,
            STREAM_POLLING_MS.milliseconds
        )

        // Check file
        try {
            VideoUtils.verifyFile(descriptor.uri.toFile())
        } catch (t: Throwable) {
            Log.e(TAG, "Error while verifying file 1", t)
            throw t
        }

        // Check video metadata
        try {
            VideoUtils.verify(context, descriptor.uri, audioConfig, videoConfig)
        } catch (t: Throwable) {
            Log.e(TAG, "Error while verifying file 2", t)
            throw t
        }

        // Delete file
        descriptor.uri.toFile().delete()
    }


    companion object {
        private const val TAG = "MultiEndpointTest"

        private const val TEST_TIMEOUT_MS = 40_000L
        private const val STREAM_DURATION_MS = 20_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
    }
}