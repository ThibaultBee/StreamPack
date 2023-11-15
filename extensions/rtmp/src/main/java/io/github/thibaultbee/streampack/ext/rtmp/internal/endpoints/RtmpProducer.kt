/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.ILiveEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.api.rtmpdroid.Rtmp
import java.security.InvalidParameterException

class RtmpProducer(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val hasAudio: Boolean = true,
    private val hasVideo: Boolean = true,
) :
    ILiveEndpoint {
    override var onConnectionListener: OnConnectionListener? = null

    private var socket = Rtmp()
    private var isOnError = false

    private var _isConnected = false
    override val isConnected: Boolean
        get() = _isConnected

    private val audioPacketQueue = mutableListOf<Packet>()

    /**
     * Sets/gets supported video codecs.
     */
    var supportedVideoCodecs: List<String>
        get() = socket.supportedVideoCodecs
        set(value) {
            socket.supportedVideoCodecs = value
        }

    override fun configure(config: Int) {
    }

    override suspend fun connect(url: String) {
        if (!url.startsWith(RTMP_PREFIX) &&
            !url.startsWith(RTMPS_PREFIX) &&
            !url.startsWith(RTMPT_PREFIX) &&
            !url.startsWith(RTMPE_PREFIX) &&
            !url.startsWith(RTMFP_PREFIX) &&
            !url.startsWith(RTMPTE_PREFIX) &&
            !url.startsWith(RTMPTS_PREFIX)
        ) {
            throw InvalidParameterException("URL must start with $RTMP_PREFIX, $RTMPS_PREFIX, $RTMPT_PREFIX, $RTMPE_PREFIX, $RTMFP_PREFIX, $RTMPTE_PREFIX or $RTMPTS_PREFIX")
        }
        withContext(coroutineDispatcher) {
            try {
                isOnError = false
                audioPacketQueue.clear()
                socket.connect("$url live=1 flashver=FMLE/3.0\\20(compatible;\\20FMSc/1.0)")
                _isConnected = true
                onConnectionListener?.onSuccess()
            } catch (e: Exception) {
                socket = Rtmp()
                _isConnected = false
                onConnectionListener?.onFailed(e.message ?: "Unknown error")
                throw e
            }
        }
    }

    override fun disconnect() {
        synchronized(this) {
            socket.close()
            _isConnected = false
            socket = Rtmp()
        }
    }

    override fun write(packet: Packet) {
        synchronized(this) {
            if (isOnError) {
                return
            }

            if (!isConnected) {
                Logger.w(TAG, "Socket is not connected, dropping packet")
                return
            }

            try {

                if (hasAudio && hasVideo) {
                    /**
                     * Audio and video packets are received out of timestamp order. We need to reorder them.
                     * We suppose that video packets arrive after audio packets.
                     * We store audio packets in a queue and send them before video packets.
                     */
                    if (packet.isAudio) {
                        // Store audio packet to send them later
                        audioPacketQueue.add(packet)
                    } else {
                        // Send audio packets
                        val audioPackets = audioPacketQueue.filter {
                            it.ts <= packet.ts
                        }

                        audioPackets.forEach { socket.write(it.buffer) }
                        audioPacketQueue.removeAll(audioPackets)

                        // Send video packet
                        socket.write(packet.buffer)
                    }
                } else {
                    socket.write(packet.buffer)
                }
            } catch (e: Exception) {
                disconnect()
                isOnError = true
                _isConnected = false
                onConnectionListener?.onLost(e.message ?: "Socket error")
                Logger.e(TAG, "Error while writing packet to socket", e)
                throw e
            }
        }
    }

    override fun startStream() {
        synchronized(this) {
            socket.connectStream()
        }
    }

    override fun stopStream() {
        synchronized(this) {
            if (isConnected) {
                /**
                 * deleteStream is blocking, if the connection does not exist yet.
                 */
                socket.deleteStream()
            }
        }
    }

    override fun release() {
    }

    companion object {
        private const val TAG = "RtmpProducer"

        private const val RTMP_SCHEME = "rtmp"
        private const val RTMP_PREFIX = "$RTMP_SCHEME://"

        private const val RTMPS_SCHEME = "rtmps"
        private const val RTMPS_PREFIX = "$RTMPS_SCHEME://"

        private const val RTMPT_SCHEME = "rtmpt"
        private const val RTMPT_PREFIX = "$RTMPT_SCHEME://"

        private const val RTMPE_SCHEME = "rtmpe"
        private const val RTMPE_PREFIX = "$RTMPE_SCHEME://"

        private const val RTMFP_SCHEME = "rtmfp"
        private const val RTMFP_PREFIX = "$RTMFP_SCHEME://"

        private const val RTMPTE_SCHEME = "rtmpte"
        private const val RTMPTE_PREFIX = "$RTMPTE_SCHEME://"

        private const val RTMPTS_SCHEME = "rtmpts"
        private const val RTMPTS_PREFIX = "$RTMPTS_SCHEME://"
    }
}