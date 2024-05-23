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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.packets

import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.data.Stream
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.utils.AssertEqualsBuffersMockMuxerListener
import io.github.thibaultbee.streampack.utils.FakeFrames
import io.github.thibaultbee.streampack.utils.MockUtils
import io.github.thibaultbee.streampack.utils.ResourcesUtils
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
    fun `single video frame to pes test`() {
        MockUtils.mockTimeUtils(1433034)

        val rawData = ResourcesUtils.readByteBuffer(TEST_SAMPLES_DIR + "pes-video1/raw")
        val frame = FakeFrames.generate(
            buffer = rawData,
            pts = 1433334,
            dts = 1400000,
            isKeyFrame = true,
            mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        )

        val expectedBuffers = readFrames(TEST_SAMPLES_DIR + "pes-video1")
        Pes(
            AssertEqualsBuffersMockMuxerListener(expectedBuffers),
            Stream(
                VideoConfig(
                    profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                    level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
                ), 256
            ),
            true
        ).run {
            write(frame)
        }
    }

    @Test
    fun `single audio frame to pes test`() {
        MockUtils.mockTimeUtils(700000)

        val rawData = ResourcesUtils.readByteBuffer(TEST_SAMPLES_DIR + "pes-audio1/raw.aac")
        val frame = FakeFrames.generate(
            buffer = rawData,
            pts = 1400000,
            dts = null,
            isKeyFrame = true,
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
        )

        val expectedBuffers = readFrames(TEST_SAMPLES_DIR + "pes-audio1")
        Pes(
            AssertEqualsBuffersMockMuxerListener(expectedBuffers),
            Stream(AudioConfig(), 256),
            true
        ).run {
            write(frame)
        }
    }

    @Test
    fun `single audio frame with stuffing length = 1 to pes test`() {
        MockUtils.mockTimeUtils(700000)

        val rawData = ResourcesUtils.readByteBuffer(TEST_SAMPLES_DIR + "pes-audio2/raw.aac")
        val frame = FakeFrames.generate(
            buffer = rawData,
            pts = 1400000,
            dts = null,
            isKeyFrame = true,
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
        )

        val expectedBuffers = readFrames(TEST_SAMPLES_DIR + "pes-audio2")
        Pes(
            AssertEqualsBuffersMockMuxerListener(expectedBuffers),
            Stream(AudioConfig(), 256),
            true
        ).run {
            write(frame)
        }
    }

    companion object {
        const val TEST_SAMPLES_DIR = "test-samples/muxer/ts/"
    }
}