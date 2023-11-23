/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.muxers

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.utils.SyncQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * An abstract class that implements [IMuxer] and output frames in their natural order.
 *
 * Frames are not in order because audio and video frames are encoded in parallel and video encoding takes more time.
 * So some new audio frame could arrive sooner than older video frame.
 *
 * Some protocols (like RTMP) require frames to be in order. Use this class for this kind of protocols.
 * If the protocol doesn't need ordered frames, use [IMuxer] directly.
 *
 * Don't call [IMuxerListener.onOutputFrame] directly, use [queue] instead.
 * Don't forget to call [stopStream] at the end of [IMuxer.stopStream] implementation.
 *
 * This implementation is based on [SyncQueue]. It waits for a video frame to send audio frames.
 * Unfortunately, sometimes video frames come faster than audio frames. So we schedule a task to
 * send the frames after a delay of [scheduleTime] [scheduleTimeUnit].
 */
abstract class AbstractSortingMuxer(
    private val scheduleTime: Long = 100,
    private val scheduleTimeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : IMuxer {
    private val syncQueue =
        SyncQueue(
            { packet1, packet2 -> packet1.ts.compareTo(packet2.ts) },
            object : SyncQueue.Listener<Packet> {
                override fun onElement(element: Packet) {
                    listener?.onOutputFrame(element)
                }
            })
    protected abstract val hasVideo: Boolean
    protected abstract val hasAudio: Boolean

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var exception: Exception? = null

    private fun asyncSyncTo(packet: Packet) {
        scheduler.schedule({
            try {
                syncQueue.syncTo(packet)
            } catch (e: Exception) {
                exception = e
            }
        }, scheduleTime, scheduleTimeUnit)
    }

    /**
     * Queues multiple packets.
     *
     * If the muxer if for video only or audio only, the packet is directly sent to
     * [IMuxerListener.onOutputFrame].
     *
     * @param packets the list of packets to queue
     */
    fun queue(packets: List<Packet>) {
        exception?.let { throw it }

        if (hasVideo && hasAudio) {
            syncQueue.add(packets)
            if (packets.any { it.isVideo }) {
                asyncSyncTo(packets.last())
            }
        } else {
            // Audio only or video only. Don't need to sort.
            packets.forEach {
                listener?.onOutputFrame(it)
            }
        }
    }

    /**
     * Queues a packet.
     *
     * If the muxer if for video only or audio only, the packet is directly sent to
     * [IMuxerListener.onOutputFrame].
     *
     * @param packet the packet to queue
     */
    fun queue(packet: Packet) {
        exception?.let { throw it }

        if (hasVideo && hasAudio) {
            syncQueue.add(packet, false)
            asyncSyncTo(packet)
        } else {
            // Audio only or video only. Don't need to sort.
            listener?.onOutputFrame(packet)
        }
    }

    /**
     * To be called at the end of [stopStream] implementation.
     */
    override fun stopStream() {
        exception = null
        syncQueue.clear()
    }
}