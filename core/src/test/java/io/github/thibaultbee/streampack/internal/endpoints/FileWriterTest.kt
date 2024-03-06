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
package io.github.thibaultbee.streampack.internal.endpoints

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.sinks.FileWriter
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.utils.FakeLogger
import io.github.thibaultbee.streampack.utils.Utils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class FileWriterTest {
    private val filePublisher = FileWriter()

    init {
        Logger.logger = FakeLogger()
    }

    @After
    fun tearDown() {
        filePublisher.release()
    }

    private fun createTestFile(): File {
        val tmpFile = File.createTempFile("test", ".tmp")
        tmpFile.deleteOnExit()
        return tmpFile
    }

    @Test
    fun `startStream with non existing file test`() {
        try {
            runBlocking{
                filePublisher.startStream()
            }
            fail("Null file must not be streamable")
        } catch (_: Exception) {
        }
    }

    @Test
    fun `write to non existing file test`() {
        try {
            val randomArray = Utils.generateRandomArray(1024)
            runBlocking{
                filePublisher.startStream()
            }
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    ts = 0
                )
            )
            fail("Null file must not be writable")
        } catch (_: Exception) {
        }
    }

    @Test
    fun `write buffer to file test`() {
        val tmpFile = createTestFile()

        filePublisher.file = tmpFile
        val randomArray = Utils.generateRandomArray(1024)
        try {
            runBlocking{
                filePublisher.startStream()
            }
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    ts = 0
                )
            )
            assertArrayEquals(randomArray, tmpFile.readBytes())
            runBlocking {
                filePublisher.stopStream()
            }
        } catch (e: Exception) {
            fail()
        }
        filePublisher.release()
    }

    @Test
    fun `write buffer to outputStream test`() {
        val tmpFile = createTestFile()

        filePublisher.outputStream = tmpFile.outputStream()
        val randomArray = Utils.generateRandomArray(1024)
        try {
            runBlocking{
                filePublisher.startStream()
            }
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    ts = 0
                )
            )
            assertArrayEquals(randomArray, tmpFile.readBytes())
            runBlocking {
                filePublisher.stopStream()
            }
        } catch (e: Exception) {
            fail()
        }
        filePublisher.release()
    }

    @Test
    fun `write direct buffer to outputStream test`() {
        val tmpFile = createTestFile()

        filePublisher.outputStream = tmpFile.outputStream()
        val randomArray = Utils.generateRandomArray(1024)
        val directBuffer = ByteBuffer.allocateDirect(1024)
        directBuffer.put(randomArray)
        directBuffer.rewind()
        try {
            runBlocking {
                filePublisher.startStream()
            }
            filePublisher.write(
                Packet(
                    directBuffer,
                    ts = 0
                )
            )
            assertArrayEquals(randomArray, tmpFile.readBytes())
            runBlocking {
                filePublisher.stopStream()
            }
        } catch (e: Exception) {
            fail()
        }
        filePublisher.release()
    }
}