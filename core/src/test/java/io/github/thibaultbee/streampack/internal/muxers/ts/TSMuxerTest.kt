/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.internal.muxers.ts

import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.muxers.ts.utils.TSConst
import io.github.thibaultbee.streampack.internal.muxers.ts.utils.Utils.createFakeServiceInfo
import io.github.thibaultbee.streampack.utils.FakeFrames
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.random.Random

class TSMuxerTest {
    class MockMuxerListener : IMuxerListener {
        override fun onOutputFrame(packet: Packet) {}
    }

    @Test
    fun `add streams in constructor test`() {
        val vStreamConfig1 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC).build()
        val vStreamConfig2 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC).build()
        val aStreamConfig =
            AudioConfig.Builder().setMimeType(MediaFormat.MIMETYPE_AUDIO_AAC).build()
        val tsMux =
            TSMuxer(
                MockMuxerListener(),
                createFakeServiceInfo(),
                listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
            )
        assertEquals(TSConst.BASE_PID, tsMux.getStreams(vStreamConfig1.mimeType)[0].pid)
        assertEquals(
            (TSConst.BASE_PID + 1).toShort(),
            tsMux.getStreams(vStreamConfig2.mimeType)[0].pid
        )
        assertEquals(
            (TSConst.BASE_PID + 2).toShort(),
            tsMux.getStreams(aStreamConfig.mimeType)[0].pid
        )
    }

    @Test
    fun `add streams test`() {
        val vStreamConfig1 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC).build()
        val vStreamConfig2 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC).build()
        val aStreamConfig =
            AudioConfig.Builder().setMimeType(MediaFormat.MIMETYPE_AUDIO_AAC).build()
        val tsMux = TSMuxer(MockMuxerListener(), createFakeServiceInfo())
        tsMux.addStreams(
            createFakeServiceInfo(),
            listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
        ).run {
            assertEquals(TSConst.BASE_PID.toInt(), this[vStreamConfig1])
            assertEquals((TSConst.BASE_PID + 1), this[vStreamConfig2])
            assertEquals((TSConst.BASE_PID + 2), this[aStreamConfig])
        }
    }

    @Test
    fun `constructor with streams and no service test`() {
        val vStreamConfig1 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC).build()
        val vStreamConfig2 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC).build()
        val aStreamConfig =
            AudioConfig.Builder().setMimeType(MediaFormat.MIMETYPE_AUDIO_AAC).build()
        try {
            TSMuxer(
                MockMuxerListener(),
                initialStreams = listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
            )
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `re-add existing service test`() {
        val service = createFakeServiceInfo()
        val tsMux = TSMuxer(MockMuxerListener(), service)
        try {
            tsMux.addService(service)
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `remove existing service test`() {
        val vStreamConfig1 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC).build()
        val vStreamConfig2 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC).build()
        val aStreamConfig =
            AudioConfig.Builder().setMimeType(MediaFormat.MIMETYPE_AUDIO_AAC).build()
        val tsMux =
            TSMuxer(
                MockMuxerListener(),
                createFakeServiceInfo(),
                listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
            )
        assertTrue(tsMux.getServices().contains(createFakeServiceInfo()))

        tsMux.removeService(createFakeServiceInfo())
        assertFalse(tsMux.getServices().contains(createFakeServiceInfo()))

        // Assert streams doe not exist
        assertTrue(tsMux.getStreams(vStreamConfig1.mimeType).isEmpty())
        assertTrue(tsMux.getStreams(vStreamConfig2.mimeType).isEmpty())
        assertTrue(tsMux.getStreams(aStreamConfig.mimeType).isEmpty())
        try { // try to add a stream to this service
            tsMux.addStreams(createFakeServiceInfo(), listOf(vStreamConfig1))
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `remove streams test `() {
        val vStreamConfig1 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC).build()
        val vStreamConfig2 =
            VideoConfig.Builder().setMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC).build()
        val aStreamConfig =
            AudioConfig.Builder().setMimeType(MediaFormat.MIMETYPE_AUDIO_AAC).build()
        val tsMux =
            TSMuxer(
                MockMuxerListener(),
                createFakeServiceInfo(),
                listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
            )

        tsMux.removeStreams(
            createFakeServiceInfo(),
            listOf(tsMux.getStreams(vStreamConfig1.mimeType)[0].pid)
        )
        assertTrue(tsMux.getStreams(vStreamConfig1.mimeType).isEmpty())
    }


    @Test
    fun `encode without streams test`() {
        val tsMux = TSMuxer(MockMuxerListener(), createFakeServiceInfo())
        try {
            tsMux.encode(FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_VIDEO_AVC), -1)
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `encode with key frame with extra test `() {
        val tsMux = TSMuxer(MockMuxerListener(), createFakeServiceInfo())
        try {
            tsMux.encode(
                Frame(
                    ByteBuffer.wrap(Random.nextBytes(1024)),
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    Random.nextLong(),
                    isKeyFrame = true
                ), -1
            )
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `encode h264 frame test`() {
        val config = VideoConfig.Builder().build()
        val tsMux = TSMuxer(MockMuxerListener(), createFakeServiceInfo())
        val streamPid =
            tsMux.addStreams(createFakeServiceInfo(), listOf(config))[config]!!

        tsMux.encode(
            FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )

        tsMux.encode(
            FakeFrames.createFakeFrame(MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )
    }

    @Test
    fun `encode aac frame test`() {
        val config = AudioConfig.Builder().build()
        val tsMux = TSMuxer(MockMuxerListener(), createFakeServiceInfo())
        val streamPid =
            tsMux.addStreams(createFakeServiceInfo(), listOf(config))[config]!!

        tsMux.encode(
            FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
        )
        tsMux.encode(
            FakeFrames.createFakeFrame(MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
        )
    }
}