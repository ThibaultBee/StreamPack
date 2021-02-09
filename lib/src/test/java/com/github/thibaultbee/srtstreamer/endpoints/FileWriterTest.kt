package com.github.thibaultbee.srtstreamer.endpoints

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.util.*

class FileWriterTest {
    fun createTempFile(): File {
        val tmpFile = File.createTempFile("test", ".tmp")
        tmpFile.deleteOnExit()
        return tmpFile
    }

    @Test
    fun fileExistTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter(file = tmpFile)
        assertTrue(tmpFile.exists())
        filePublisher.close()
    }

    @Test
    fun writeToFileTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter(file = tmpFile)
        val random = UUID.randomUUID().toString()
        filePublisher.write(ByteBuffer.wrap(random.toByteArray()))
        assertEquals(random, tmpFile.readText())
        filePublisher.close()
    }
}