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
package io.github.thibaultbee.streampack.core.pipelines.outputs.encoding

import android.content.Context
import android.media.MediaFormat
import androidx.core.net.toFile
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpointDummyFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpointFactory
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.pipelines.outputs.releaseBlocking
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class EncodingPipelineOutputTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var output: EncodingPipelineOutput

    private val descriptor by lazy { UriMediaDescriptor("file://test.mp4") }

    @After
    fun tearDown() {
        try {
            output.releaseBlocking()
        } catch (_: Throwable) {
        }
        descriptor.uri.toFile().delete()
    }

    @Test
    fun testOpenClose() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        output.open(descriptor)
        assertTrue(output.isOpenFlow.value)
        output.close()
        assertFalse(output.isOpenFlow.value)
    }

    @Test
    fun testSetAudioCodecConfig() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        assertNull(output.audioSourceConfigFlow.value)

        suspendCoroutine { continuation ->
            val listener = object : IConfigurableAudioPipelineOutputInternal.Listener {
                override suspend fun onSetAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig) {
                    continuation.resume(newAudioSourceConfig)
                }
            }
            output.audioConfigEventListener = listener

            runBlocking {
                output.setAudioCodecConfig(AudioCodecConfig())
            }
        }
        assertNotNull(output.audioSourceConfigFlow.value)
    }

    @Test
    fun testSetAudioCodecConfigAndReject() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        output.audioConfigEventListener =
            object : IConfigurableAudioPipelineOutputInternal.Listener {
                override suspend fun onSetAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig) {
                    throw Exception()
                }
            }
        try {
            output.setAudioCodecConfig(AudioCodecConfig())
            fail("Should throw an exception")
        } catch (_: Throwable) {
            assertNull(output.audioSourceConfigFlow.value)
        }
    }

    @Test
    fun testSetVideoCodecConfig() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        assertNull(output.videoCodecConfigFlow.value)

        suspendCoroutine { continuation ->
            val listener = object : IConfigurableVideoPipelineOutputInternal.Listener {
                override suspend fun onSetVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig) {
                    continuation.resume(newVideoSourceConfig)
                }
            }
            output.videoConfigEventListener = listener

            runBlocking {
                output.setVideoCodecConfig(VideoCodecConfig())
            }
        }
        assertNotNull(output.videoCodecConfigFlow.value)
    }

    @Test
    fun testSetVideoCodecConfigAndReject() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        output.videoConfigEventListener =
            object : IConfigurableVideoPipelineOutputInternal.Listener {
                override suspend fun onSetVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig) {
                    throw Exception()
                }
            }
        try {
            output.setVideoCodecConfig(VideoCodecConfig())
            fail("Should throw an exception")
        } catch (_: Throwable) {
            assertNull(output.audioSourceConfigFlow.value)
        }
    }

    @Test
    fun testStartStreamWithoutConfig() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        try {
            output.startStream(descriptor)
            fail("Should throw an exception")
        } catch (_: Throwable) {
            assertFalse(output.isStreaming)
        }
    }

    @Test
    fun testStartStreamWithAudioConfig() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        output.setAudioCodecConfig(AudioCodecConfig())
        output.startStream(descriptor)
    }

    @Test
    fun testStartStreamWithVideoConfig() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        output.setVideoCodecConfig(VideoCodecConfig())
        output.startStream(descriptor)
    }

    @Test
    fun testStartStream() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        output.setVideoCodecConfig(VideoCodecConfig()) // at least one config is needed

        assertFalse(output.isStreaming)

        suspendCoroutine { continuation ->
            val listener = object : IPipelineOutputInternal.Listener {
                override suspend fun onStartStream() {
                    continuation.resume(Unit)
                }
            }
            output.streamEventListener = listener
            runBlocking {
                output.startStream(descriptor)
            }
        }
        assertTrue(output.isOpenFlow.value)
        assertTrue(output.isStreaming)
        output.stopStream()
        assertTrue(output.isOpenFlow.value)
        assertFalse(output.isStreaming)
        output.close()
        assertFalse(output.isOpenFlow.value)
    }

    @Test
    fun testStartStreamAndReject() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())

        output.setVideoCodecConfig(VideoCodecConfig()) // at least one config is needed

        assertFalse(output.isStreaming)
        output.streamEventListener = object : IPipelineOutputInternal.Listener {
            override suspend fun onStartStream() {
                throw Exception()
            }
        }
        try {
            output.startStream(descriptor)
            fail("Should throw an exception")
        } catch (_: Throwable) {
            assertFalse(output.isOpenFlow.value)
            assertFalse(output.isStreaming)
        }
    }

    @Test
    fun testStartStreamVideoCodecSurface() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        assertNull(output.surfaceFlow.value)

        output.setVideoCodecConfig(VideoCodecConfig())
        val firstSurface = output.surfaceFlow.value
        assertNotNull(firstSurface)

        output.startStream(descriptor)
        output.stopStream()

        /**
         * A surface is emitted each time after [EncodingPipelineOutput.stopStream] on a different
         * thread.
         */
        val secondSurface = output.surfaceFlow.filterNotNull().first()
        assertNotNull(secondSurface)
        assertNotEquals(firstSurface, secondSurface)
    }

    @Test
    fun testClose() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        output.close()
    }

    @Test
    fun testStopStreamOnly() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        output.stopStream()
    }

    @Test
    fun testReleaseOnly() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        output.release()
    }

    // Tests on inputs

    @Test
    fun testSetVideoCodecSurface() = runTest {
        output = EncodingPipelineOutput(context, endpointInternalFactory = DummyEndpointFactory())
        assertNull(output.surfaceFlow.value)

        output.setVideoCodecConfig(VideoCodecConfig())
        val firstSurface = output.surfaceFlow.value
        assertNotNull(firstSurface)

        output.setVideoCodecConfig(VideoCodecConfig()) // Same config the surface should be the same
        val secondSurface = output.surfaceFlow.value
        assertNotNull(secondSurface)
        assertEquals(firstSurface, output.surfaceFlow.value)

        output.setVideoCodecConfig(VideoCodecConfig(fps = 24))
        val thirdSurface = output.surfaceFlow.value
        assertNotNull(thirdSurface)
        assertNotEquals(firstSurface, thirdSurface)
    }

    @Test
    fun testQueueAudioFrame() = runTest {
        val dummyEndpoint = DummyEndpoint()
        output = EncodingPipelineOutput(
            context,
            endpointInternalFactory = DummyEndpointDummyFactory(dummyEndpoint)
        )

        try {
            output.queueAudioFrame(
                Frame(
                    ByteBuffer.allocateDirect(16384),
                    pts = Random.nextLong(),
                    format = MediaFormat().apply {
                        setString(
                            MediaFormat.KEY_MIME,
                            MediaFormat.MIMETYPE_AUDIO_RAW
                        )
                    }
                )
            )
            fail("Should throw an exception")
        } catch (_: Throwable) {
        }

        output.setAudioCodecConfig(AudioCodecConfig())
        output.startStream(descriptor)

        output.queueAudioFrame(
            Frame(
                ByteBuffer.allocateDirect(16384),
                pts = Random.nextLong(),
                format = MediaFormat().apply {
                    setString(
                        MediaFormat.KEY_MIME,
                        MediaFormat.MIMETYPE_AUDIO_RAW
                    )
                }
            )
        )

        assertTrue(dummyEndpoint.frameFlow.filterNotNull().first().isAudio)
    }
}