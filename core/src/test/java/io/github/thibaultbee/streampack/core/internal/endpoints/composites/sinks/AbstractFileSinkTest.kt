/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks

import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.utils.FakeLogger
import io.github.thibaultbee.streampack.core.internal.utils.FileUtils
import io.github.thibaultbee.streampack.core.internal.utils.Utils
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
abstract class AbstractFileSinkTest(val sink: ISink) {
    init {
        Logger.logger = FakeLogger()
    }

    @Test
    fun `stopStream only must not throw`() = runTest {
        sink.stopStream()
    }

    @Test
    fun `close only must not throw`() = runTest {
        sink.close()
    }

    @Test
    fun `startStream with non existing file test`() = runTest {
        try {
            sink.startStream()
            fail("Null file must not be streamable")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `write to non initialized file test`() = runTest {
        val randomArray = Utils.generateRandomArray(1024)

        try {
            sink.write(
                Packet(
                    ByteBuffer.wrap(randomArray),
                    ts = 0
                )
            )
            fail("Null file must not be writable")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `open write buffer to file test`() = runTest {
        val tmpFile = FileUtils.createTestFile(".mp4")
        val randomArray = byteArrayOf(0, 1, 2, 3, 4)

        sink.open(
            UriMediaDescriptor(
                tmpFile.toUri()
            )
        )
        sink.startStream()
        val byteWritten = sink.write(
            Packet(
                ByteBuffer.wrap(randomArray),
                ts = 0
            )
        )
        sink.stopStream()
        assertArrayEquals(randomArray, tmpFile.readBytes())
        assertEquals(randomArray.size, byteWritten)
    }

    @Test
    fun `write multi buffer to file test with start stop`() = runTest {
        val tmpFile = FileUtils.createTestFile(".mp4")
        val randomArray1 = byteArrayOf(0, 1, 2, 3, 4)
        val randomArray2 = byteArrayOf(5, 6, 7, 8, 9)

        sink.open(
            UriMediaDescriptor(
                tmpFile.toUri()
            )
        )
        sink.startStream()
        var byteWritten = sink.write(
            Packet(
                ByteBuffer.wrap(randomArray1),
                ts = 0
            )
        )
        sink.stopStream()
        assertArrayEquals(randomArray1, tmpFile.readBytes())
        assertEquals(randomArray1.size, byteWritten)

        sink.startStream()
        byteWritten = sink.write(
            Packet(
                ByteBuffer.wrap(randomArray2),
                ts = 0
            )
        )
        sink.stopStream()
        assertArrayEquals(randomArray1 + randomArray2, tmpFile.readBytes())
        assertEquals(randomArray2.size, byteWritten)
    }

    @Test
    fun `write buffer to multiple files`() = runTest {
        val tmpFile1 = FileUtils.createTestFile(".mp4")
        val tmpFile2 = FileUtils.createTestFile(".mp4")
        val randomArray1 = byteArrayOf(0, 1, 2, 3, 4)
        val randomArray2 = byteArrayOf(5, 6, 7, 8, 9)

        sink.open(
            UriMediaDescriptor(
                tmpFile1.toUri()
            )
        )
        sink.startStream()
        var byteWritten = sink.write(
            Packet(
                ByteBuffer.wrap(randomArray1),
                ts = 0
            )
        )
        sink.stopStream()
        sink.close()

        // Open second file
        sink.open(
            UriMediaDescriptor(
                tmpFile2.toUri()
            )
        )
        sink.startStream()
        byteWritten = sink.write(
            Packet(
                ByteBuffer.wrap(randomArray2),
                ts = 0
            )
        )
        sink.stopStream()
        sink.close()

        assertArrayEquals(randomArray2, tmpFile2.readBytes())
        assertEquals(randomArray2.size, byteWritten)
        assertArrayEquals(randomArray1, tmpFile1.readBytes())
        assertEquals(randomArray1.size, byteWritten)
    }

    @Test
    fun `multiple opens`() = runTest {
        val tmpFile = FileUtils.createTestFile(".mp4")
        sink.open(
            UriMediaDescriptor(
                tmpFile.toUri()
            )
        )

        try {
            sink.open(
                UriMediaDescriptor(
                    tmpFile.toUri()
                )
            )
            fail("Sink must not be openable twice")
        } catch (_: Throwable) {
        }
    }
}