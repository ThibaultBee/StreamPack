package com.github.thibaultbee.streampack.muxers.ts

import android.media.MediaFormat
import com.github.thibaultbee.streampack.data.Frame
import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.muxers.IMuxerListener
import com.github.thibaultbee.streampack.muxers.ts.utils.TSConst
import com.github.thibaultbee.streampack.muxers.ts.utils.Utils.fakeServiceInfo
import com.github.thibaultbee.streampack.utils.FakeFrames
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.random.Random

class TSMuxerTest {
    class MockMuxerListener : IMuxerListener {
        override fun onOutputFrame(packet: Packet) {}
    }

    @Test
    fun `test full constructor`() {
        val vstream1 = "video/test"
        val vstream2 = "video/test2"
        val astream2 = "audio/test"
        val tsMux =
            TSMuxer(MockMuxerListener(), fakeServiceInfo(), listOf(vstream1, vstream2, astream2))
        assertEquals(TSConst.BASE_PID, tsMux.getStreams(vstream1)[0].pid)
        assertEquals((TSConst.BASE_PID + 1).toShort(), tsMux.getStreams(vstream2)[0].pid)
        assertEquals((TSConst.BASE_PID + 2).toShort(), tsMux.getStreams(astream2)[0].pid)
    }

    @Test
    fun `test no streams constructor`() {
        val vstream1 = "video/test"
        val vstream2 = "video/test2"
        val astream2 = "audio/test"
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
        tsMux.addStreams(fakeServiceInfo(), listOf(vstream1, vstream2, astream2)).run {
            assertEquals(TSConst.BASE_PID, this[0])
            assertEquals((TSConst.BASE_PID + 1).toShort(), this[1])
            assertEquals((TSConst.BASE_PID + 2).toShort(), this[2])
        }
    }


    @Test
    fun `test no service but streams constructor`() {
        val vstream1 = "video/test"
        val vstream2 = "video/test2"
        val astream2 = "audio/test"
        try {
            TSMuxer(MockMuxerListener(), initialStreams = listOf(vstream1, vstream2, astream2))
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `test failed on re-add existing service`() {
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
        try {
            tsMux.addService(fakeServiceInfo())
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `test remove existing service`() {
        val vstream1 = "video/test"
        val vstream2 = "video/test2"
        val astream2 = "audio/test"
        val tsMux =
            TSMuxer(MockMuxerListener(), fakeServiceInfo(), listOf(vstream1, vstream2, astream2))
        assertTrue(tsMux.getService().contains(fakeServiceInfo()))

        tsMux.removeService(fakeServiceInfo())
        assertFalse(tsMux.getService().contains(fakeServiceInfo()))

        // Assert streams doe not exist
        assertTrue(tsMux.getStreams(vstream1).isEmpty())
        assertTrue(tsMux.getStreams(vstream2).isEmpty())
        assertTrue(tsMux.getStreams(astream2).isEmpty())
        try { // try to add a stream to this service
            tsMux.addStreams(fakeServiceInfo(), listOf("video/test"))
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `test remove streams`() {
        val vstream1 = "video/test"
        val vstream2 = "video/test2"
        val astream2 = "audio/test"
        val tsMux =
            TSMuxer(MockMuxerListener(), fakeServiceInfo(), listOf(vstream1, vstream2, astream2))

        tsMux.removeStreams(fakeServiceInfo(), listOf(tsMux.getStreams(vstream1)[0].pid))
        assertTrue(tsMux.getStreams(vstream1).isEmpty())
    }


    @Test
    fun `test encode without streams`() {
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
        try {
            tsMux.encode(FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_VIDEO_AVC), -1)
            fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `test encode with key frame with extra`() {
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
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
    fun `test encode h264 frame`() {
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
        val streamPid =
            tsMux.addStreams(fakeServiceInfo(), listOf(MediaFormat.MIMETYPE_VIDEO_AVC))[0]

        tsMux.encode(
            FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )

        tsMux.encode(
            FakeFrames.createFakeFrame(MediaFormat.MIMETYPE_VIDEO_AVC), streamPid
        )
    }

    @Test
    fun `test encode aac frame`() {
        val tsMux = TSMuxer(MockMuxerListener(), fakeServiceInfo())
        val streamPid =
            tsMux.addStreams(fakeServiceInfo(), listOf(MediaFormat.MIMETYPE_AUDIO_AAC))[0]

        tsMux.encode(
            FakeFrames.createFakeKeyFrame(MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
        )
        try {
            tsMux.encode(
                FakeFrames.createFakeFrame(MediaFormat.MIMETYPE_AUDIO_AAC), streamPid
            )
            fail()
        } catch (e: Exception) {
        }
    }
}