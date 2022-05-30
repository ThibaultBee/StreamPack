package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class AVCConfigurationBoxTest {
    @Test
    fun `write valid avcC test`() {
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x67,
                0x42,
                0xC0.toByte(),
                0x32,
                0xDA.toByte(),
                0x00,
                0x80.toByte(),
                0x02,
                0x46,
                0xC0.toByte(),
                0x44,
                0x00,
                0x00,
                0x03,
                0x00,
                0x04,
                0x00,
                0x00,
                0x03,
                0x00,
                0xF2.toByte(),
                0x3C,
                0x60,
                0xCA.toByte(),
                0x80.toByte()
            )
        )
        val pps = ByteBuffer.wrap(byteArrayOf(0x68, 0xCE.toByte(), 0x09, 0xC8.toByte()))
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("avcC.box")
        val avcC = AVCConfigurationBox(
            AVCDecoderConfigurationRecord(
                profileIdc = 66,
                profileCompatibility = 192.toByte(),
                levelIdc = 50,
                sps = listOf(sps),
                pps = listOf(pps)
            )
        )
        val buffer = avcC.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}