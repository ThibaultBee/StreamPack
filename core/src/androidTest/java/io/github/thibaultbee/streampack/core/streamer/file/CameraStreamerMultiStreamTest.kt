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
package io.github.thibaultbee.streampack.core.streamer.file

import android.Manifest
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Test [DefaultCameraStreamer] with multiple streams.
 */
@LargeTest
class CameraStreamerMultiStreamTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private val streamer = DefaultCameraStreamer(context)

    private val descriptors = listOf(
        UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()),
        UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri())
    )

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @Test
    fun writeToEndpoints() = runTest(timeout = 200.seconds) {
        val audioConfig = AudioConfig()
        val videoConfig = VideoConfig(resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT))

        // Run stream
        streamer.configure(
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
        StreamerUtils.runStream(
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
    }


    companion object {
        private const val TAG = "MultiEndpointTest"

        private const val STREAM_DURATION_MS = 20_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
    }
}