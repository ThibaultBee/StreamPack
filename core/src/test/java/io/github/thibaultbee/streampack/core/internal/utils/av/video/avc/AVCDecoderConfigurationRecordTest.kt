package io.github.thibaultbee.streampack.core.internal.utils.av.video.avc

import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.core.internal.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class AVCDecoderConfigurationRecordTest {
    @Test
    fun `write valid AVCDecoderConfigurationRecord from constructor test`() {
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
        val expectedBuffer =
            ResourcesUtils.readByteBuffer("test-samples/video/AVCDecoderConfigurationRecord")
        val avcDecoderConfigurationRecord =
            AVCDecoderConfigurationRecord(
                profileIdc = 66,
                profileCompatibility = 192.toByte(),
                levelIdc = 50,
                sps = listOf(sps),
                pps = listOf(pps)
            )

        val buffer = ByteBuffer.allocateDirect(avcDecoderConfigurationRecord.size)
        avcDecoderConfigurationRecord.write(buffer)
        buffer.rewind()

        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }

    @Test
    fun `write valid AVCDecoderConfigurationRecord from companion with start code test`() {
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x00,
                0x00,
                0x00,
                0x01,
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
        val pps = ByteBuffer.wrap(
            byteArrayOf(
                0x00, 0x00, 0x00, 0x01, 0x68, 0xCE.toByte(), 0x09, 0xC8.toByte()
            )
        )
        val expectedBuffer =
            ResourcesUtils.readByteBuffer("test-samples/video/AVCDecoderConfigurationRecord")
        val avcDecoderConfigurationRecord =
            AVCDecoderConfigurationRecord.fromParameterSets(sps, pps)

        val buffer = ByteBuffer.allocateDirect(avcDecoderConfigurationRecord.size)
        avcDecoderConfigurationRecord.write(buffer)
        buffer.rewind()

        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }

    @Test
    fun `write valid AVCDecoderConfigurationRecord from companion without start code test`() {
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
        val pps = ByteBuffer.wrap(
            byteArrayOf(
                0x68, 0xCE.toByte(), 0x09, 0xC8.toByte()
            )
        )
        val expectedBuffer =
            ResourcesUtils.readByteBuffer("test-samples/video/AVCDecoderConfigurationRecord")
        val avcDecoderConfigurationRecord =
            AVCDecoderConfigurationRecord.fromParameterSets(sps, pps)

        val buffer = ByteBuffer.allocateDirect(avcDecoderConfigurationRecord.size)
        avcDecoderConfigurationRecord.write(buffer)
        buffer.rewind()

        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}