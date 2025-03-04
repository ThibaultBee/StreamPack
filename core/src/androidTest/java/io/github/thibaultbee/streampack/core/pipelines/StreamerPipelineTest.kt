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

import android.content.Context
import android.media.MediaFormat
import android.util.Size
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.StubAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.StubVideoSurfaceSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.StubAudioSyncConfigurableEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.StubAudioSyncVideoSurfacePipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.StubAudioSyncVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.StubVideoSurfaceConfigurableEncodingPipelineOutputInternal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StreamerPipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var streamerPipeline: StreamerPipeline

    @After
    fun tearDown() {
        try {
            streamerPipeline.releaseBlocking()
        } catch (_: Throwable) {
        }
    }

    private suspend fun buildStreamerPipeline(
        context: Context,
        audioSource: IAudioSourceInternal?,
        videoSource: IVideoSourceInternal?
    ): StreamerPipeline {
        val pipeline = StreamerPipeline(
            context,
            hasAudio = audioSource != null,
            hasVideo = videoSource != null
        )
        audioSource?.let { pipeline.setAudioSource(it) }
        videoSource?.let { pipeline.setVideoSource(it) }
        return pipeline
    }

    // Test on source
    @Test
    fun testStartStreamSources() = runTest {
        val timestampOffset = 1234L

        val audioSource = StubAudioSource()
        val videoSource = StubVideoSurfaceSource(timestampOffsetInNs = timestampOffset)

        val output = StubAudioSyncVideoSurfacePipelineOutputInternal(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(context, audioSource, videoSource)
        streamerPipeline.addOutput(output)

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(audioSource.isStreamingFlow.value)
        assertTrue(videoSource.isStreamingFlow.value)

        streamerPipeline.stopStream()
        assertFalse(streamerPipeline.isStreamingFlow.first { !it })
        assertFalse(audioSource.isStreamingFlow.value)
        assertFalse(videoSource.isStreamingFlow.value)
    }

    // Test on add/remove output
    @Test
    fun testRemoveOutput() = runTest {
        val output = StubAudioSyncVideoSurfacePipelineOutputInternal(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(output)

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.value)
        assertTrue(output.isStreamingFlow.value)

        streamerPipeline.removeOutput(output)
        assertFalse(streamerPipeline.isStreamingFlow.value)
        assertFalse(output.isStreamingFlow.value)

        // Release output
        try {
            output.release()
        } catch (_: Throwable) {
        }
    }

    // Test on config
    @Test
    fun testWith2CompatibleAudioCodecConfig() = runTest {
        val firstOutput = StubAudioSyncConfigurableEncodingPipelineOutputInternal()
        val secondOutput = StubAudioSyncConfigurableEncodingPipelineOutputInternal()

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        firstOutput.setAudioCodecConfig(AudioCodecConfig(startBitrate = 128_000))

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        secondOutput.setAudioCodecConfig(AudioCodecConfig(startBitrate = 64_000))
    }

    @Test
    fun testWith2IncompatibleAudioCodecConfig() = runTest {
        val firstOutput = StubAudioSyncConfigurableEncodingPipelineOutputInternal()
        val secondOutput = StubAudioSyncConfigurableEncodingPipelineOutputInternal()

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        firstOutput.setAudioCodecConfig(AudioCodecConfig(sampleRate = 48000))

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        try {
            secondOutput.setAudioCodecConfig(AudioCodecConfig(sampleRate = 44100))
            fail("StreamerPipeline should not accept incompatible audio config")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun testWith2CompatibleVideoCodecConfig() = runTest {
        val firstOutput = StubVideoSurfaceConfigurableEncodingPipelineOutputInternal()
        val secondOutput = StubVideoSurfaceConfigurableEncodingPipelineOutputInternal()

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        firstOutput.setVideoCodecConfig(VideoCodecConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_AVC))

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        secondOutput.setVideoCodecConfig(VideoCodecConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC))
    }

    @Test
    fun testWith2IncompatibleVideoCodecConfig() = runTest {
        val firstOutput = StubVideoSurfaceConfigurableEncodingPipelineOutputInternal()
        val secondOutput = StubVideoSurfaceConfigurableEncodingPipelineOutputInternal()

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        firstOutput.setVideoCodecConfig(VideoCodecConfig(fps = 30))

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        try {
            secondOutput.setVideoCodecConfig(VideoCodecConfig(fps = 25))
            fail("StreamerPipeline should not accept incompatible video config")
        } catch (_: Throwable) {
        }
    }

    // Test on start/stop stream
    // No output
    @Test
    fun testWithoutOutput() = runTest {
        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )

        try {
            streamerPipeline.startStream()
            fail("StreamerPipeline should not start without output")
        } catch (_: Throwable) {
        }
    }

    // Single output
    @Test
    fun testStartStreamSingleIsStreamingOutput() = runTest {
        val output = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(output)

        output.startStream()
        /**
         * In case the [output] is not a [ISyncStartStreamPipelineOutputInternal], the
         * [streamerPipeline] is started asynchronously by a collector on [output.isStreamingFlow].
         */
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(output.isStreamingFlow.value)

        output.stopStream()
        assertFalse(streamerPipeline.isStreamingFlow.first { !it })
        assertFalse(output.isStreamingFlow.value)
    }

    @Test
    fun testStartStreamSingleStartStreamListenerOutput() = runTest {
        val output = StubAudioSyncVideoSurfacePipelineOutputInternal(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(output)

        output.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(output.isStreamingFlow.value)

        output.stopStream()
        // Stop stream is asynchronous
        assertFalse(streamerPipeline.isStreamingFlow.first { !it })
        assertFalse(output.isStreamingFlow.value)
    }

    // Multiple outputs
    @Test
    fun testStartStreamAllOutputs() = runTest {
        val firstOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))
        val secondOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(firstOutput.isStreamingFlow.value)
        assertTrue(secondOutput.isStreamingFlow.value)

        streamerPipeline.stopStream()
        assertFalse(streamerPipeline.isStreamingFlow.first { !it })
        assertFalse(firstOutput.isStreamingFlow.value)
        assertFalse(secondOutput.isStreamingFlow.value)
    }

    @Test
    fun testStartStreamOneOfMultipleOutputs() = runTest {
        val firstOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))
        val secondOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        firstOutput.startStream()

        // Second output should not start streaming
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(firstOutput.isStreamingFlow.value)
        assertFalse(secondOutput.isStreamingFlow.value)

        firstOutput.stopStream()
        assertFalse(streamerPipeline.isStreamingFlow.first { !it })
        assertFalse(firstOutput.isStreamingFlow.value)
        assertFalse(secondOutput.isStreamingFlow.value)
    }

    @Test
    fun testStartStreamAllOutputsStopStreamOne() = runTest {
        val firstOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))
        val secondOutput = StubAudioSyncVideoSurfacePipelineOutput(resolution = Size(640, 480))

        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.addOutput(firstOutput)
        streamerPipeline.addOutput(secondOutput)

        streamerPipeline.startStream()
        assertTrue(streamerPipeline.isStreamingFlow.first { it })
        assertTrue(firstOutput.isStreamingFlow.value)
        assertTrue(secondOutput.isStreamingFlow.value)

        firstOutput.stopStream()
        assertTrue(streamerPipeline.isStreamingFlow.value)
        assertFalse(firstOutput.isStreamingFlow.value)
        assertTrue(secondOutput.isStreamingFlow.value)
    }

    // Test on destructive methods
    @Test
    fun testStopStream() = runTest {
        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.stopStream()
    }

    @Test
    fun testRelease() = runTest {
        streamerPipeline = buildStreamerPipeline(
            context,
            StubAudioSource(),
            StubVideoSurfaceSource()
        )
        streamerPipeline.release()
    }
}