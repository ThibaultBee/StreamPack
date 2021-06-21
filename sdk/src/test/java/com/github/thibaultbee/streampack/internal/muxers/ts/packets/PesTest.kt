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
package com.github.thibaultbee.streampack.internal.muxers.ts.packets

import android.media.MediaFormat
import com.github.thibaultbee.streampack.internal.data.Frame
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Stream
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.AssertEqualsBuffersMockMuxerListener
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class PesTest {
    /**
     * Read all files with prefix "frame" and suffix ".ts" from specified directory
     * @param resourcesDirStr expected buffers (often pre-generated buffers)
     * @return list of ByteBuffers containing all ts packet
     */
    private fun readFrames(resourcesDirStr: String): List<ByteBuffer> {
        val resourcesDirFile =
            File(this.javaClass.classLoader!!.getResources(resourcesDirStr).nextElement().toURI())

        val buffers = mutableListOf<ByteBuffer>()
        resourcesDirFile.listFiles()!!.toList()
            .filter { it.name.startsWith("frame") && it.name.endsWith(".ts") }
            .sorted()
            .forEach { buffers.add(ByteBuffer.wrap(it.readBytes())) }


        return buffers
    }

    @Test
    fun testSimpleVideoFrame() {
        val rawData = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/pes-video1/raw")!!
                .readBytes()
        )
        val frame =
            Frame(rawData, MediaFormat.MIMETYPE_VIDEO_AVC, 1433334, 1400000, isKeyFrame = true)

        val expectedBuffers = readFrames("test-samples/pes-video1")
        Pes(
            AssertEqualsBuffersMockMuxerListener(expectedBuffers),
            Stream(MediaFormat.MIMETYPE_VIDEO_AVC, 256),
            true
        ).run {
            write(frame)
        }
    }

    @Test
    fun testSimpleAudioFrame() {
        val rawData = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/pes-audio1/raw.aac")!!
                .readBytes()
        )
        val frame =
            Frame(rawData, MediaFormat.MIMETYPE_AUDIO_AAC, 1400000, null, isKeyFrame = true)

        val expectedBuffers = readFrames("test-samples/pes-audio1")
        Pes(
            AssertEqualsBuffersMockMuxerListener(expectedBuffers),
            Stream(MediaFormat.MIMETYPE_AUDIO_AAC, 256),
            true
        ).run {
            write(frame)
        }
    }
}