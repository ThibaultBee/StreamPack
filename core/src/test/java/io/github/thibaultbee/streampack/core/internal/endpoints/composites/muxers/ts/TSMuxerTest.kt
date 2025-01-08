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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts

import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.internal.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.internal.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.utils.TSConst
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.utils.Utils.createFakeServiceInfo
import io.github.thibaultbee.streampack.core.internal.utils.FakeFrames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TSMuxerTest {
    class MockMuxerListener :
        io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxerInternal.IMuxerListener {
        override fun onOutputFrame(packet: io.github.thibaultbee.streampack.core.internal.data.Packet) {}
    }

    @Test
    fun `add streams test`() {
        val vStreamConfig1 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val vStreamConfig2 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val aStreamConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC)
        val service = createFakeServiceInfo()

        val tsMux =
            TSMuxer().apply {
                addService(service)
                addStreams(
                    service,
                    listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
                )
            }
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
    fun `constructor with streams and no service test`() {
        val vStreamConfig1 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val vStreamConfig2 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val aStreamConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC)
        try {
            TSMuxer().apply {
                addStreams(
                    listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
                )
            }
            fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `re-add existing service test`() {
        val service = createFakeServiceInfo()
        val tsMux = TSMuxer().apply {
            addService(service)
        }
        try {
            tsMux.addService(service)
            fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `remove existing service test`() {
        val vStreamConfig1 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val vStreamConfig2 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val aStreamConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC)
        val service = createFakeServiceInfo()

        val tsMux =
            TSMuxer().apply {
                addService(service)
                addStreams(
                    service,
                    listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
                )
            }
        assertTrue(tsMux.getServices().contains(service))

        tsMux.removeService(service)
        assertFalse(tsMux.getServices().contains(service))

        // Assert streams doe not exist
        assertTrue(tsMux.getStreams(vStreamConfig1.mimeType).isEmpty())
        assertTrue(tsMux.getStreams(vStreamConfig2.mimeType).isEmpty())
        assertTrue(tsMux.getStreams(aStreamConfig.mimeType).isEmpty())
        try { // try to add a stream to this service
            tsMux.addStreams(service, listOf(vStreamConfig1))
            fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `remove streams test `() {
        val vStreamConfig1 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val vStreamConfig2 =
            VideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC,
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        val aStreamConfig = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC)
        val service = createFakeServiceInfo()

        val tsMux =
            TSMuxer().apply {
                addService(service)
                addStreams(
                    service,
                    listOf(vStreamConfig1, vStreamConfig2, aStreamConfig)
                )
            }

        tsMux.removeStreams(
            service,
            listOf(tsMux.getStreams(vStreamConfig1.mimeType)[0].pid)
        )
        assertTrue(tsMux.getStreams(vStreamConfig1.mimeType).isEmpty())
    }


    @Test
    fun `encode without streams test`() {
        val tsMux = TSMuxer()
        try {
            tsMux.write(FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_VIDEO_AVC), -1)
            fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `encode with key frame with extra test `() {
        val tsMux = TSMuxer()
        try {
            tsMux.write(
                FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_VIDEO_AVC), -1
            )
            fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `encode h264 frame test`() {
        val config = VideoCodecConfig(
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
            profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
            level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
        )
        val service = createFakeServiceInfo()

        val tsMux = TSMuxer().apply {
            addService(service)
        }
        val streamPid =
            tsMux.addStreams(service, listOf(config))[config]!!

        tsMux.write(
            FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )

        tsMux.write(
            FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )
    }

    @Test
    fun `encode aac frame test`() {
        val config = AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC)
        val service = createFakeServiceInfo()

        val tsMux = TSMuxer().apply {
            addService(service)
        }
        val streamPid =
            tsMux.addStreams(createFakeServiceInfo(), listOf(config))[config]!!

        tsMux.write(
            FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
        )
        tsMux.write(
            FakeFrames.generate(mimeType = MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
        )
    }
}