package com.github.thibaultbee.srtstreamer.mux.ts.packets

import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.data.Frame
import com.github.thibaultbee.srtstreamer.mux.ts.data.Stream
import com.github.thibaultbee.srtstreamer.mux.ts.utils.AssertEqualsBuffersMockMuxListener
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class PesTest {
    /**
     * Read all files with prefix "frame" and suffix ".ts" from specified directory
     * @param dir expected buffers (often pre-generated buffers)
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
            AssertEqualsBuffersMockMuxListener(expectedBuffers),
            Stream(MediaFormat.MIMETYPE_VIDEO_AVC, 256),
            true
        ).run {
            write(frame)
        }
    }
}