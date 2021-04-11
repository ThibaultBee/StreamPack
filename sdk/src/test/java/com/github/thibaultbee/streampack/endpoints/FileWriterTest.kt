package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.utils.Utils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class FileWriterTest {
    private fun createTempFile(): File {
        val tmpFile = File.createTempFile("test", ".tmp")
        tmpFile.deleteOnExit()
        return tmpFile
    }

    @Test
    fun defaultFileExistTest() {
        val filePublisher = FileWriter()
        assertTrue(filePublisher.file.exists())
        filePublisher.release()
    }

    @Test
    fun fileExistTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter()
        filePublisher.file = tmpFile
        assertTrue(tmpFile.exists())
        filePublisher.release()
    }

    @Test
    fun writeToFileTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter()
        filePublisher.file = tmpFile
        val randomArray = Utils.generateRandomArray(1024)
        filePublisher.write(
            Packet(
                ByteBuffer.wrap(randomArray),
                isFirstPacketFrame = true,
                isLastPacketFrame = true,
                ts = 0
            )
        )
        assertArrayEquals(randomArray, tmpFile.readBytes())
        filePublisher.release()
    }
}