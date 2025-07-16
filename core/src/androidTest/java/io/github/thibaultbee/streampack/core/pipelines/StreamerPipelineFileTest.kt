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
package io.github.thibaultbee.streampack.core.pipelines

import android.media.MediaFormat
import android.util.Log
import android.util.Size
import androidx.core.net.toUri
import androidx.test.filters.LargeTest
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.streamer.utils.VideoUtils
import io.github.thibaultbee.streampack.core.utils.DeviceTest
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@LargeTest
class StreamerPipelineFileTest : DeviceTest() {
    private val streamerPipeline by lazy {
        StreamerPipeline(
            context,
            withAudio = true,
            withVideo = true
        )
    }

    @After
    fun tearDown() {
        try {
            streamerPipeline.releaseBlocking()
            Log.d(TAG, "StreamerPipeline released")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun testAudioOrVideoStream() = runTest(timeout = TEST_TIMEOUT_MS.milliseconds) {
        // Add sources
        streamerPipeline.setAudioSource(MicrophoneSourceFactory())
        streamerPipeline.setVideoSource(CameraSourceFactory())

        // Add outputs
        val audioOnlyOutput =
            streamerPipeline.createEncodingOutput(withVideo = false) as IConfigurableAudioEncodingPipelineOutput
        val videoOnlyOutput =
            streamerPipeline.createEncodingOutput(withAudio = false) as IConfigurableVideoEncodingPipelineOutput

        // Configure outputs
        val audioConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
        val videoConfig = VideoCodecConfig(
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
            resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT)
        )

        audioOnlyOutput.setAudioCodecConfig(audioConfig)
        videoOnlyOutput.setVideoCodecConfig(videoConfig)

        val audioOnlyDescriptor = UriMediaDescriptor(FileUtils.createCacheFile("audio.ogg").toUri())
        val videoOnlyDescriptor = UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri())

        // Run stream
        audioOnlyOutput.startStream(audioOnlyDescriptor)
        withContext(Dispatchers.Default) {
            delay(10_000)
        }
        videoOnlyOutput.startStream(videoOnlyDescriptor)

        val duration: Duration = STREAM_DURATION_MS.milliseconds
        val pollDuration: Duration = STREAM_POLLING_MS.milliseconds
        var i = 0
        val numOfLoop = duration / pollDuration
        withContext(Dispatchers.Default) {
            while (i < numOfLoop) {
                i++
                delay(pollDuration)
                assertTrue(streamerPipeline.isStreamingFlow.value)
                assertTrue(audioOnlyOutput.isStreamingFlow.value)
                assertTrue(videoOnlyOutput.isStreamingFlow.value)
            }
        }

        streamerPipeline.stopStream()
        audioOnlyOutput.close()
        videoOnlyOutput.close()
        streamerPipeline.release()
        
        // Verify
        VideoUtils.verifyFile(
            context,
            audioOnlyDescriptor.uri,
            true,
            audioConfig,
            null
        )
        VideoUtils.verifyFile(
            context,
            videoOnlyDescriptor.uri,
            true,
            null,
            videoConfig
        )
    }

    companion object {
        private const val TAG = "StreamerPipelineFileTest"

        private const val TEST_TIMEOUT_MS = 60_000L
        private const val STREAM_DURATION_MS = 30_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
    }
}