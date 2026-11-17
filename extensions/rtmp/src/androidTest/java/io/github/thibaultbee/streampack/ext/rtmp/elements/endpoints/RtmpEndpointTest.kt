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
package io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Extra
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.io.EOFException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import kotlin.concurrent.thread

class RtmpEndpointTest {
    private val server = PublishDelayingRtmpServer(publishDelayMs = 500)

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun framesQueuedWhilePublishIsPendingStartTheConnectionWithTheSequenceHeader() = runBlocking {
        val endpoint = RtmpEndpoint(Dispatchers.Default, Dispatchers.IO)
        try {
            endpoint.open(UriMediaDescriptor("rtmp://127.0.0.1:${server.port}/app/key"))
            val audioPid = endpoint.addStream(AudioCodecConfig())
            val pid = endpoint.addStream(VideoCodecConfig())

            // startStream() holds the connection until publish is answered: tags queue meanwhile.
            val start = launch { endpoint.startStream() }
            delay(100)
            endpoint.write(AudioFrame(), audioPid)
            endpoint.write(VideoFrame(isKeyFrame = true, ptsInUs = 0), pid)
            for (i in 1..10) {
                endpoint.write(VideoFrame(isKeyFrame = false, ptsInUs = i * 33_000L), pid)
            }
            start.join()

            withTimeout(5_000) {
                while (!server.video.contains("330 inter nalu")) delay(10)
            }
            assertEquals(
                "first video messages",
                listOf("0 key seqhdr", "0 key nalu"),
                server.video.take(2)
            )
        } finally {
            endpoint.close()
            endpoint.release()
        }
    }

    @Test
    fun sequenceHeaderDroppedOverTheByteBudgetIsSentWithTheNextKeyFrame() = runBlocking {
        val endpoint = RtmpEndpoint(Dispatchers.Default, Dispatchers.IO)
        try {
            endpoint.open(UriMediaDescriptor("rtmp://127.0.0.1:${server.port}/app/key"))
            val audioPid = endpoint.addStream(AudioCodecConfig())
            val pid = endpoint.addStream(VideoCodecConfig())

            val start = launch { endpoint.startStream() }
            delay(100)
            // The writer takes the first tag and waits with it, so the video header stays queued.
            endpoint.write(AudioFrame(), audioPid)
            endpoint.write(VideoFrame(isKeyFrame = true, ptsInUs = 0), pid)
            // 10 x 20 kB exceeds the budget (0.5 s of the default bitrates): the oldest tags go.
            for (i in 1..10) {
                endpoint.write(
                    VideoFrame(isKeyFrame = false, ptsInUs = i * 33_000L, size = 20_000),
                    pid
                )
            }
            start.join()

            endpoint.write(VideoFrame(isKeyFrame = true, ptsInUs = 400_000), pid)
            withTimeout(5_000) {
                while (!server.video.contains("400 key nalu")) delay(10)
            }
            assertEquals(
                "video messages",
                listOf("400 key seqhdr", "400 key nalu"),
                server.video
            )
        } finally {
            endpoint.close()
            endpoint.release()
        }
    }

    private class AudioFrame : Frame {
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

    private class VideoFrame(
        override val isKeyFrame: Boolean,
        override val ptsInUs: Long,
        size: Int = 7
    ) : Frame {
        override val rawBuffer: ByteBuffer = ByteBuffer.wrap(
            byteArrayOf(0, 0, 0, 1, if (isKeyFrame) 0x65 else 0x41) + ByteArray(size - 5) { 0x11 }
        )
        override val dtsInUs: Long? = null
        override val extra = Extra(listOf(ByteBuffer.wrap(SPS), ByteBuffer.wrap(PPS)))
        override val format = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
        }

        override fun close() = Unit
    }

    /**
     * Accepts one RTMP publisher, answers `publish` after [publishDelayMs] and records each video
     * message as "<timestamp> <frame type> <AVC packet type>".
     */
    private class PublishDelayingRtmpServer(private val publishDelayMs: Long) {
        private val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val port = serverSocket.localPort
        val video: MutableList<String> = Collections.synchronizedList(mutableListOf())

        @Volatile
        private var socket: Socket? = null

        init {
            thread(isDaemon = true) {
                try {
                    serverSocket.accept().use {
                        socket = it
                        Connection(it).run()
                    }
                } catch (_: Exception) {
                }
            }
        }

        fun close() {
            socket?.close()
            serverSocket.close()
        }

        private inner class Connection(socket: Socket) {
            private val input = DataInputStream(socket.getInputStream())
            private val output = socket.getOutputStream()
            private var inChunkSize = 128
            private val headers = mutableMapOf<Int, IntArray>() // ts, length, type, delta
            private val partial = mutableMapOf<Int, ByteArray>()

            private fun read(n: Int) = ByteArray(n).also { input.readFully(it) }
            private fun readInt(n: Int) =
                read(n).fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }

            // Every reply fits in one 128-byte chunk.
            private fun send(csid: Int, type: Int, msid: Int, payload: ByteArray) {
                val header = ByteBuffer.allocate(12).put(csid.toByte()).put(byteArrayOf(0, 0, 0))
                    .putInt((payload.size shl 8) or type)
                    .order(ByteOrder.LITTLE_ENDIAN).putInt(msid)
                synchronized(output) { output.write(header.array() + payload) }
            }

            private fun amfString(s: String) = byteArrayOf(2, 0, s.length.toByte()) + s.toByteArray()

            private fun amfNumber(v: Double) =
                byteArrayOf(0) + ByteBuffer.allocate(8).putDouble(v).array()

            private fun amfObject(vararg entries: Pair<String, String>) =
                entries.fold(byteArrayOf(3)) { acc, (k, v) ->
                    acc + byteArrayOf(0, k.length.toByte()) + k.toByteArray() + amfString(v)
                } + byteArrayOf(0, 0, 9)

            private fun onCommand(payload: ByteArray) {
                val nameLength = ((payload[1].toInt() and 0xFF) shl 8) or (payload[2].toInt() and 0xFF)
                val name = String(payload, 3, nameLength)
                val txn = ByteBuffer.wrap(payload, 4 + nameLength, 8).double
                val result = amfString("_result") + amfNumber(txn)
                when (name) {
                    "connect" -> {
                        send(2, 5, 0, ByteBuffer.allocate(4).putInt(2_500_000).array())
                        send(2, 6, 0, ByteBuffer.allocate(5).putInt(2_500_000).put(2).array())
                        send(
                            3, 20, 0, result + amfObject("fmsVer" to "FMS/3,0,1,123") +
                                    amfObject(
                                        "level" to "status",
                                        "code" to "NetConnection.Connect.Success"
                                    )
                        )
                    }

                    "createStream" -> send(3, 20, 0, result + byteArrayOf(5) + amfNumber(1.0))
                    "publish" -> thread(isDaemon = true) {
                        Thread.sleep(publishDelayMs)
                        send(
                            5, 20, 1, amfString("onStatus") + amfNumber(0.0) + byteArrayOf(5) +
                                    amfObject(
                                        "level" to "status",
                                        "code" to "NetStream.Publish.Start"
                                    )
                        )
                    }

                    "deleteStream", "closeStream" -> Unit
                    else -> if (txn != 0.0) send(3, 20, 0, result + byteArrayOf(5, 5))
                }
            }

            private fun onMessage(type: Int, ts: Int, payload: ByteArray) {
                when (type) {
                    1 -> inChunkSize = ByteBuffer.wrap(payload).int and 0x7FFFFFFF
                    20 -> onCommand(payload)
                    9 -> {
                        val frameType = if (payload[0].toInt() shr 4 == 1) "key" else "inter"
                        val packetType = if (payload[1].toInt() == 0) "seqhdr" else "nalu"
                        video.add("$ts $frameType $packetType")
                    }
                }
            }

            fun run() {
                val c0c1 = read(1537)
                output.write(byteArrayOf(3) + ByteArray(1536) + c0c1.copyOfRange(1, 1537))
                read(1536)
                try {
                    while (true) {
                        val b0 = readInt(1)
                        val fmt = b0 shr 6
                        val csid = when (val id = b0 and 0x3F) {
                            0 -> 64 + readInt(1)
                            1 -> read(2).let { 64 + (it[0].toInt() and 0xFF) + (it[1].toInt() and 0xFF) * 256 }
                            else -> id
                        }
                        val h = headers.getOrPut(csid) { IntArray(5) } // ts, length, type, msid, delta
                        var t = 0
                        if (fmt <= 2) t = readInt(3)
                        if (fmt <= 1) {
                            h[1] = readInt(3)
                            h[2] = readInt(1)
                        }
                        if (fmt == 0) h[3] = Integer.reverseBytes(readInt(4))
                        if (fmt <= 2 && t == 0xFFFFFF) t = readInt(4)
                        if (fmt == 0) {
                            h[0] = t
                            h[4] = 0
                        } else if (fmt <= 2) {
                            h[4] = t
                        }
                        val buffer = partial[csid] ?: ByteArray(0)
                        if (buffer.isEmpty() && fmt != 0) h[0] += h[4]
                        val chunk = buffer + read(minOf(inChunkSize, h[1] - buffer.size))
                        if (chunk.size >= h[1]) {
                            partial.remove(csid)
                            onMessage(h[2], h[0], chunk)
                        } else {
                            partial[csid] = chunk
                        }
                    }
                } catch (_: EOFException) {
                }
            }
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
