package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.MockUtils
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class AVCSampleEntryTest {
    @Test
    fun `write valid avc1 test`() {
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x67,
                0x64,
                0x00,
                0x1F,
                0xAC.toByte(),
                0xD9.toByte(),
                0xC0.toByte(),
                0x44,
                0x03,
                0xC7.toByte(),
                0x88.toByte(),
                0xE1.toByte(),
                0x00,
                0x00,
                0x03,
                0x00,
                0x01,
                0x00,
                0x00,
                0x03,
                0x00,
                0x3C,
                0x0F,
                0x18,
                0x31,
                0x9E.toByte()
            )
        )
        val pps =
            ByteBuffer.wrap(byteArrayOf(0x68, 0xE9.toByte(), 0xBB.toByte(), 0x2C, 0x8B.toByte()))
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("avc1.box")
        val avcC = AVCConfigurationBox(
            AVCDecoderConfigurationRecord(
                profileIdc = 100,
                profileCompatibility = 0,
                levelIdc = 31,
                sps = listOf(sps),
                pps = listOf(pps)
            )
        )
        val btrt = BitRateBox(1100000, 4840000, 3878679)

        val avc1 =
            AVCSampleEntry(MockUtils.mockSize(1074, 1920), avcc = avcC, btrt = btrt)
        val buffer = avc1.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}