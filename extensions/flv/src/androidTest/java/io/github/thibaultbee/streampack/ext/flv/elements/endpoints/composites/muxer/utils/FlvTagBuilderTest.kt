/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils

import android.media.MediaFormat
import io.github.komedia.komuxer.flv.tags.FLVTag
import io.github.thibaultbee.streampack.core.elements.data.Extra
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.utils.ChannelWithCloseableData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class FlvTagBuilderTest {
    private val channel = ChannelWithCloseableData<FLVTag>(16)
    private val builder = FlvTagBuilder(channel)

    private fun received(): Int = receivedTags(channel).size

    private fun receivedTags(channel: ChannelWithCloseableData<FLVTag>): List<FLVTag> {
        val tags = mutableListOf<FLVTag>()
        while (true) {
            val element = channel.tryReceive().getOrNull() ?: break
            element.close()
            tags.add(element.data)
        }
        return tags
    }

    /** A builder whose 2-tag channel drops its oldest tag on overflow, like the endpoints'. */
    private fun overflowingBuilder(): Pair<FlvTagBuilder, ChannelWithCloseableData<FLVTag>> {
        lateinit var builder: FlvTagBuilder
        val channel = ChannelWithCloseableData<FLVTag>(
            2,
            BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = { builder.onTagDropped(it) }
        )
        builder = FlvTagBuilder(channel)
        return builder to channel
    }

    @Test
    fun videoBeforeTheFirstKeyFrameIsDroppedAndClosed() = runTest {
        val pid = builder.addStream(VideoCodecConfig())

        val inter = TestFrame(isKeyFrame = false)
        builder.write(inter, 0, pid)
        assertEquals("tags for an inter frame before any keyframe", 0, received())
        assertTrue("dropped frame is closed", inter.isClosed)

        builder.write(TestFrame(isKeyFrame = true), 33, pid)
        assertEquals("sequence header + keyframe", 2, received())

        builder.write(TestFrame(isKeyFrame = false), 66, pid)
        assertEquals("inter frame after the keyframe", 1, received())
    }

    @Test
    fun droppedVideoSequenceHeaderIsSentAgainWithTheNextKeyFrame() = runTest {
        val (builder, channel) = overflowingBuilder()
        val pid = builder.addStream(VideoCodecConfig())

        builder.write(TestFrame(isKeyFrame = true), 0, pid)
        builder.write(TestFrame(isKeyFrame = false), 33, pid) // overflows: header dropped
        assertEquals("keyframe + inter frame left after the overflow", 2, receivedTags(channel).size)

        builder.write(TestFrame(isKeyFrame = false), 66, pid)
        assertEquals("tags for an inter frame after a dropped header", 0, receivedTags(channel).size)

        builder.write(TestFrame(isKeyFrame = true), 100, pid)
        assertEquals("tags for the keyframe after a dropped header", 2, receivedTags(channel).size)
    }

    @Test
    fun droppedAudioSequenceHeaderIsSentAgainWithTheNextFrame() = runTest {
        val (builder, channel) = overflowingBuilder()
        val pid = builder.addStream(AudioCodecConfig())

        builder.write(TestAudioFrame(), 0, pid)
        builder.write(TestAudioFrame(), 23, pid) // overflows: header dropped
        assertEquals("audio frames left after the overflow", 2, receivedTags(channel).size)

        builder.write(TestAudioFrame(), 46, pid)
        assertEquals("tags for the audio frame after a dropped header", 2, receivedTags(channel).size)
    }

    @Test
    fun onlyAnUnwrittenSequenceHeaderReArmsIt() = runTest {
        val pid = builder.addStream(VideoCodecConfig())

        builder.write(TestFrame(isKeyFrame = true), 0, pid)
        val (header, keyFrame) = receivedTags(channel)
        builder.onTagDropped(keyFrame)
        builder.write(TestFrame(isKeyFrame = false), 33, pid)
        assertEquals("tags for an inter frame after a dropped keyframe tag", 1, received())

        builder.onTagDropped(header) // e.g. its write timed out
        builder.write(TestFrame(isKeyFrame = false), 66, pid)
        assertEquals("tags for an inter frame after a failed header write", 0, received())
        builder.write(TestFrame(isKeyFrame = true), 100, pid)
        assertEquals("tags for the keyframe after a failed header write", 2, received())
    }

    private class TestAudioFrame : Frame {
        override val rawBuffer: ByteBuffer = ByteBuffer.wrap(byteArrayOf(0x21, 0x10, 0x05))
        override val ptsInUs = 0L
        override val dtsInUs: Long? = null
        override val isKeyFrame = true
        override val extra = Extra(listOf(ByteBuffer.wrap(byteArrayOf(0x12, 0x10))))
        override val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        }

        override fun close() = Unit
    }

    private class TestFrame(override val isKeyFrame: Boolean) : Frame {
        var isClosed = false
            private set
        override val rawBuffer: ByteBuffer =
            ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1, if (isKeyFrame) 0x65 else 0x41, 0x11, 0x22))
        override val ptsInUs = 0L
        override val dtsInUs: Long? = null
        override val extra = Extra(listOf(ByteBuffer.wrap(SPS), ByteBuffer.wrap(PPS)))
        override val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
        }

        override fun close() {
            isClosed = true
        }
    }

    companion object {
        private val SPS = byteArrayOf(
            0, 0, 0, 1, 0x67, 0x42, 0xC0.toByte(), 0x32, 0xDA.toByte(), 0x00, 0x80.toByte(), 0x02,
            0x46, 0xC0.toByte(), 0x44, 0x00, 0x00, 0x03, 0x00, 0x04, 0x00, 0x00, 0x03, 0x00,
            0xF2.toByte(), 0x3C, 0x60, 0xCA.toByte(), 0x80.toByte()
        )
        private val PPS = byteArrayOf(0, 0, 0, 1, 0x68, 0xCE.toByte(), 0x09, 0xC8.toByte())
    }
}
