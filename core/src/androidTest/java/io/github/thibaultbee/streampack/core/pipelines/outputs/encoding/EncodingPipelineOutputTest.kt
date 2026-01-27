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
import android.util.Log
import androidx.core.net.toFile
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.MutableRawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpointDummyFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.DummyEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
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
            Log.e(TAG, "Release")
            output.releaseBlocking()
        } catch (t: Throwable) {
            Log.e(TAG, "Release failed with $t", t)
        }
        descriptor.uri.toFile().delete()
    }

    private fun createOutput(
        endpointFactory: IEndpointInternal.Factory,
        withAudio: Boolean = true,
        withVideo: Boolean = true,
    ): EncodingPipelineOutput {
        return EncodingPipelineOutput(
            context = context,
            withAudio = withAudio,
            withVideo = withVideo,
            endpointFactory = endpointFactory,
            defaultRotation = context.displayRotation,
            dispatcherProvider = DispatcherProvider()
        )
    }

    @Test
    fun testOpenClose() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
        output.open(descriptor)
        assertTrue(output.isOpenFlow.value)
        output.close()
        assertFalse(output.isOpenFlow.value)
    }

    @Test
    fun testSetAudioCodecConfig() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
        assertNull(output.audioSourceConfigFlow.value)

        val sourceConfig = suspendCoroutine { continuation ->
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
        assertNotNull(output.audioSourceConfigFlow.first { it == sourceConfig })
    }

    @Test
    fun testSetAudioCodecConfigAndReject() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
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
        output = createOutput(endpointFactory = DummyEndpointFactory())
        assertNull(output.videoCodecConfigFlow.value)

        val sourceConfig = suspendCoroutine { continuation ->
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
        assertNotNull(output.videoSourceConfigFlow.first { it == sourceConfig })
    }

    @Test
    fun testSetVideoCodecConfigAndReject() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
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
        output = createOutput(endpointFactory = DummyEndpointFactory())
        try {
            output.startStream(descriptor)
            fail("Should throw an exception")
        } catch (_: Throwable) {
            assertFalse(output.isStreaming)
        }
    }

    @Test
    fun testStartStreamWithAudioConfig() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory(), withVideo = false)

        output.setAudioCodecConfig(AudioCodecConfig())
        output.startStream(descriptor)
    }

    @Test
    fun testStartStreamWithVideoConfig() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory(), withAudio = false)

        output.setVideoCodecConfig(VideoCodecConfig())
        output.startStream(descriptor)
    }

    @Test
    fun testStartStream() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory(), withAudio = false)

        output.setVideoCodecConfig(VideoCodecConfig()) // at least one config is needed

        assertFalse(output.isStreaming)

        suspendCoroutine { continuation ->
            val listener = object : IPipelineEventOutputInternal.Listener {
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
        output = createOutput(endpointFactory = DummyEndpointFactory())

        output.setVideoCodecConfig(VideoCodecConfig()) // at least one config is needed

        assertFalse(output.isStreaming)
        output.streamEventListener = object : IPipelineEventOutputInternal.Listener {
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
        output = createOutput(endpointFactory = DummyEndpointFactory(), withAudio = false)
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
        output = createOutput(endpointFactory = DummyEndpointFactory())
        output.close()
    }

    @Test
    fun testStopStreamOnly() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
        output.stopStream()
    }

    @Test
    fun testReleaseOnly() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
        output.release()
    }

    // Tests on inputs

    @Test
    fun testSetVideoCodecSurface() = runTest {
        output = createOutput(endpointFactory = DummyEndpointFactory())
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
        output = createOutput(
            endpointFactory = DummyEndpointDummyFactory(dummyEndpoint),
            withVideo = false,
        )

        try {
            output.queueAudioFrame(
                MutableRawFrame(
                    ByteBuffer.allocateDirect(16384),
                    Random.nextLong()
                )
            )
            fail("Should throw an exception")
        } catch (_: Throwable) {
        }

        output.setAudioCodecConfig(AudioCodecConfig())
        output.startStream(descriptor)

        output.queueAudioFrame(
            MutableRawFrame(
                ByteBuffer.allocateDirect(16384),
                Random.nextLong()
            )
        )
    }

    companion object {
        private const val TAG = "EncodingPipelineOutputTest"
    }
}