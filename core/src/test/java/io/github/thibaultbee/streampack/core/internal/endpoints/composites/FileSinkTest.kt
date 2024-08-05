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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites

import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.internal.utils.FakeLogger
import io.github.thibaultbee.streampack.core.internal.utils.Utils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
class FileSinkTest {
    private val filePublisher = FileSink()

    init {
        Logger.logger = FakeLogger()
    }

    private fun createTestFile(suffix: String = ".tmp"): File {
        val tmpFile = File.createTempFile("test", suffix)
        tmpFile.deleteOnExit()
        return tmpFile
    }

    @Test
    fun `startStream with non existing file test`() = runTest {
        try {
            filePublisher.startStream()
            fail("Null file must not be streamable")
        } catch (_: Exception) {
        }
    }

    @Test
    fun `write to non existing file test`() = runTest {
        val randomArray = Utils.generateRandomArray(1024)

        try {
            filePublisher.startStream()
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
    fun `write buffer to file test`() = runTest {
        val tmpFile = createTestFile(".mp4")
        val randomArray = Utils.generateRandomArray(1024)

        filePublisher.open(
            UriMediaDescriptor(
                tmpFile.toUri()
            )
        )
        try {
            filePublisher.startStream()
            filePublisher.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    ts = 0
                )
            )
            assertArrayEquals(randomArray, tmpFile.readBytes())
            filePublisher.stopStream()
        } catch (e: Exception) {
            fail("Failed to write buffer to file: ${e.message}")
        }
    }

    /* TODO
    @Test
    fun `write buffer to outputStream test`() = runTest{
        val tmpFile = createTestFile()

        filePublisher.outputStream = tmpFile.outputStream()
        val randomArray = Utils.generateRandomArray(1024)
        try {
            runBlocking {
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
    }*/
}