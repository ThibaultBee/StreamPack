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
package com.github.thibaultbee.streampack.internal.endpoints

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.utils.FakeLogger
import com.github.thibaultbee.streampack.utils.Utils
import org.junit.Assert.*
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
    fun fileExistTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter(FakeLogger())
        filePublisher.file = tmpFile
        assertTrue(tmpFile.exists())
        filePublisher.release()
    }

    @Test
    fun startStreamWithNullFileTest() {
        val filePublisher = FileWriter(FakeLogger())
        try {
            filePublisher.startStream()
            fail("Null file must not be streamable")
        } catch (e: Exception) {

        }
    }

    @Test
    fun writeToNullFileTest() {
        val filePublisher = FileWriter(FakeLogger())
        try {
            val randomArray = Utils.generateRandomArray(1024)
            filePublisher.startStream()
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    isFirstPacketFrame = true,
                    isLastPacketFrame = true,
                    ts = 0
                )
            )
            fail("Null file must not be writable")
        } catch (e: Exception) {

        }
    }

    @Test
    fun writeToFileTest() {
        val tmpFile = createTempFile()
        val filePublisher = FileWriter(FakeLogger())
        filePublisher.file = tmpFile
        val randomArray = Utils.generateRandomArray(1024)
        try {
            filePublisher.startStream()
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    isFirstPacketFrame = true,
                    isLastPacketFrame = true,
                    ts = 0
                )
            )
            assertArrayEquals(randomArray, tmpFile.readBytes())
            filePublisher.stopStream()
        } catch (e: Exception) {
            fail()
        }
        filePublisher.release()
    }
}