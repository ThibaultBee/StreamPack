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
package io.github.thibaultbee.streampack.internal.muxers.ts

import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.muxers.ts.data.Service
import io.github.thibaultbee.streampack.internal.muxers.ts.data.Stream
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.muxers.ts.packets.Pat
import io.github.thibaultbee.streampack.internal.muxers.ts.packets.Pes
import io.github.thibaultbee.streampack.internal.muxers.ts.packets.Pmt
import io.github.thibaultbee.streampack.internal.muxers.ts.packets.Sdt
import io.github.thibaultbee.streampack.internal.muxers.ts.utils.MuxerConst
import io.github.thibaultbee.streampack.internal.muxers.ts.utils.TSConst
import io.github.thibaultbee.streampack.internal.utils.av.audio.aac.ADTSFrameWriter
import io.github.thibaultbee.streampack.internal.utils.av.audio.aac.LATMFrameWriter
import java.nio.ByteBuffer
import java.util.MissingFormatArgumentException
import kotlin.random.Random

class TSMuxer(
    initialListener: IMuxerListener? = null,
    initialTsServiceInfo: TsServiceInfo? = null,
    initialStreams: List<Config>? = null,
) : IMuxer {
    override val helper = TSMuxerHelper()
    private val tsServices = mutableListOf<Service>()
    private val tsPes = mutableListOf<Pes>()
    override var sourceOrientationProvider: ISourceOrientationProvider? = null
    override var listener: IMuxerListener? = initialListener
        set(value) {
            pat.listener = value
            sdt.listener = value
            tsPes.forEach { it.listener = value }
            tsServices.forEach { it.pmt?.listener = value }

            field = value
        }

    private val tsId = Random.nextInt(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toShort()
    private var pat = Pat(
        listener,
        tsServices,
        tsId,
        packetCount = 0
    )
    private var sdt = Sdt(
        listener,
        tsServices,
        tsId,
        packetCount = 0
    )

    init {
        if (initialStreams != null) {
            require(initialTsServiceInfo != null) { "If streams are specified, a service info must be specified too" }
        }
        initialTsServiceInfo?.let { addService(it) }
        initialStreams?.let { addStreams(tsServices[0], it) }
    }

    /**
     * Encode a frame to MPEG-TS format.
     * Each audio frames and each video key frames must come with an extra buffer containing sps, pps,...
     * @param frame frame to mux
     * @param streamPid Pid of frame stream. Throw a NoSuchElementException if streamPid refers to an unknown stream
     */
    override fun encode(frame: Frame, streamPid: Int) {
        val pes = getPes(streamPid.toShort())
        val newFrame = when (frame.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                // Copy sps & pps before buffer
                if (frame.isKeyFrame) {
                    if (frame.extra == null) {
                        throw MissingFormatArgumentException("Missing extra for AVC")
                    }
                    val buffer =
                        ByteBuffer.allocate(
                            6 + frame.extra.sumOf { it.limit() } + frame.buffer.limit()
                        )
                    // Add access unit delimiter (AUD) before the AVC access unit
                    buffer.putInt(0x00000001)
                    buffer.put(0x09.toByte())
                    buffer.put(0xf0.toByte())
                    frame.extra.forEach { buffer.put(it) }
                    buffer.put(frame.buffer)
                    buffer.rewind()
                    frame.copy(rawBuffer = buffer)
                } else {
                    frame
                }
            }

            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                // Copy sps & pps & vps before buffer
                if (frame.isKeyFrame) {
                    if (frame.extra == null) {
                        throw MissingFormatArgumentException("Missing extra for HEVC")
                    }
                    val buffer =
                        ByteBuffer.allocate(
                            7 + frame.extra.sumOf { it.limit() } + frame.buffer.limit()
                        )
                    // Add access unit delimiter (AUD) before the HEVC access unit
                    buffer.putInt(0x00000001)
                    buffer.put(0x46.toByte())
                    buffer.put(0x01.toByte())
                    buffer.put(0x50.toByte())
                    frame.extra.forEach { buffer.put(it) }
                    buffer.put(frame.buffer)
                    buffer.rewind()
                    frame.copy(rawBuffer = buffer)
                } else {
                    frame
                }
            }

            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                frame.copy(
                    rawBuffer =
                    if (pes.stream.config.profile == MediaCodecInfo.CodecProfileLevel.AACObjectLC) {
                        ADTSFrameWriter.fromAudioConfig(
                            frame.buffer, pes.stream.config as AudioConfig
                        ).toByteBuffer()
                    } else {
                        LATMFrameWriter.fromDecoderSpecificInfo(frame.buffer, frame.extra!!.first())
                            .toByteBuffer()
                    }
                )
            }

            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                val payload = frame.rawBuffer
                val controlHeader = opusControlHeader(payload.remaining())
                val opusAccessUnit = ByteBuffer.allocate(controlHeader.remaining() + payload.remaining())
                opusAccessUnit.put(controlHeader)
                opusAccessUnit.put(payload)
                opusAccessUnit.rewind()
                frame.copy(rawBuffer = opusAccessUnit)
            }
            else -> throw IllegalArgumentException("Unsupported mimeType ${frame.mimeType}")
        }

        synchronized(this) {
            generateStreams(newFrame, pes)
        }
    }

    private fun opusControlHeader(payloadSize: Int): ByteBuffer {
        val payloadSizeFullBytesCount = payloadSize / 255
        val payloadSizeRemainderByte = payloadSize % 255
        val headerSize = 2 + payloadSizeFullBytesCount + payloadSizeRemainderByte.coerceAtMost(1)
        val header = ByteBuffer.allocate(headerSize)

        // control_header_prefix 11 bits (0x3FF or 01111111111)
        // start_trim_flag 1 bit (0)
        // end_trim_flag 1 bit (0)
        // control_extension_flag 1 bit (0)
        // reserved 2 bits (0)
        header.put(0x7F.toByte())
        header.put(0xE0.toByte())

        repeat(payloadSizeFullBytesCount) {
            header.put(0xFF.toByte())
        }
        if (payloadSizeRemainderByte > 0) {
            header.put(payloadSizeRemainderByte.toByte())
        }

        header.rewind()
        return header
    }

    /**
     * Generate MPEG-TS table and elementary stream from the frame
     * @param pes Pes containing infos on the stream
     * @param frame frame to mux
     */
    private fun generateStreams(frame: Frame, pes: Pes) {
        retransmitPsi(frame.isVideo and frame.isKeyFrame)
        pes.write(frame)
    }

    /**
     * Manage table retransmission
     * @param forcePat Force to remit a PAT. Set to true on video key frame.
     */
    private fun retransmitPsi(forcePat: Boolean) {
        var sendSdt = false
        var sendPat = false

        sdt.packetCount += 1
        if (sdt.packetCount == MuxerConst.SDT_PACKET_PERIOD) {
            sdt.packetCount = 0
            sendSdt = true
        }

        pat.packetCount += 1
        if ((pat.packetCount == MuxerConst.PAT_PACKET_PERIOD) || forcePat) {
            pat.packetCount = 0
            sendPat = true
        }

        if (sendSdt) {
            sendSdt()
        }
        if (sendPat) {
            sendPat()
            sendPmts()
        }
    }

    private fun upgradePat() {
        pat.versionNumber = (pat.versionNumber + 1).toByte()
        sendPat()
    }

    private fun sendPat() {
        pat.write()
    }


    private fun sendPmt(service: Service) {
        service.pmt?.write() ?: throw UnsupportedOperationException("PMT must not be null")
    }

    private fun sendPmts() {
        tsServices.filter { it.pmt != null }.forEach {
            it.pmt?.write() ?: throw UnsupportedOperationException("PMT must not be null")
        }
    }

    private fun upgradeSdt() {
        sdt.versionNumber = (sdt.versionNumber + 1).toByte()
        sendSdt()
    }

    private fun sendSdt() {
        sdt.write()
    }

    /**
     * Get registered services list
     * @return list of registered services
     */
    fun getServices(): List<TsServiceInfo> {
        return tsServices.map { it.info }
    }

    /**
     * Register a new service. Service will be valid (tables will be emitted) as soon as streams will be added.
     * @param tsServiceInfo new service to add to service list
     */
    fun addService(tsServiceInfo: TsServiceInfo) {
        require(tsServices.none { it.info == tsServiceInfo }) { "Service already exists" }

        tsServices.add(Service(tsServiceInfo))
    }

    /**
     * Remove a service and its streams
     * @param tsServiceInfo service info of service to remove
     */
    fun removeService(tsServiceInfo: TsServiceInfo) = removeService(getServices(tsServiceInfo))

    /**
     * Remove a service and its streams
     * @param service service to remove
     */
    private fun removeService(service: Service) {
        require(tsServices.contains(service)) { "Unknown service" }
        tsServices.remove(service)

        if (service.streams.isNotEmpty()) {
            removeStreams(service, service.streams)
        }

        upgradeSdt()
        upgradePat()
    }

    /**
     * Add streams to the first service registered
     *
     * @param streamsConfig list of config
     * @return ordered list of stream id
     */
    override fun addStreams(streamsConfig: List<Config>) =
        addStreams(getServices()[0], streamsConfig)


    /**
     * Add streams for a service
     * @param tsServiceInfo service where to add streams
     * @param streamsConfig list of config
     * @return ordered list of stream id
     */
    fun addStreams(tsServiceInfo: TsServiceInfo, streamsConfig: List<Config>) =
        addStreams(getServices(tsServiceInfo), streamsConfig)


    /**
     * Add streams for a service
     * @param service service where to add streams
     * @param streamsConfig list of config
     * @return list of corresponding PES
     */
    private fun addStreams(service: Service, streamsConfig: List<Config>): Map<Config, Int> {
        require(tsServices.contains(service)) { "Unknown service" }

        val isNewService = service.pmt == null

        val newStreams = mutableListOf<Stream>()
        streamsConfig.forEach {
            val stream = Stream(it, getNewPid())
            newStreams.add(stream)
            service.streams.add(stream)
        }

        service.pcrPid = try {
            service.streams.first { it.isVideo }.pid
        } catch (e: NoSuchElementException) {
            service.streams[0].pid
        }

        // Prepare tables
        service.pmt = service.pmt?.apply {
            versionNumber = (versionNumber + 1).toByte()
            streams = service.streams
        } ?: Pmt(listener, service, service.streams, getNewPid())

        // Init PES
        newStreams.forEach {
            Pes(
                listener,
                it,
                service.pcrPid == it.pid,
            ).run { tsPes.add(this) }
        }

        // Send tables
        sendPmt(service)
        if (isNewService) {
            upgradeSdt()
            upgradePat()
        }

        val streamMap = mutableMapOf<Config, Int>()
        newStreams.forEach { streamMap[it.config] = it.pid.toInt() }
        return streamMap
    }

    /**
     * Remove streams from service.
     *
     * @param tsServiceInfo service info
     * @param streamsPid list of streams to remove
     */
    fun removeStreams(tsServiceInfo: TsServiceInfo, streamsPid: List<Short>) =
        removeStreams(getServices(tsServiceInfo), streamsPid.map { getStream(it) })

    /**
     * Remove streams from service.
     *
     * @param service service
     * @param streams list of streams to remove
     */
    private fun removeStreams(service: Service, streams: List<Stream>) {
        streams.forEach {
            tsPes.remove(getPes(it.pid))
        }
        service.streams.removeAll(streams)

        service.pmt?.let {
            it.versionNumber = (it.versionNumber + 1).toByte()
            it.streams = service.streams
        }

        sendPmt(service)
    }

    /**
     * Remove all streams from a service.
     *
     * @param service service
     */
    private fun removeStreams(service: Service) {
        service.streams.forEach {
            tsPes.remove(getPes(it.pid))
        }
        service.streams.clear()
    }

    override fun startStream() {
        // Nothing to start
    }

    /**
     * Clear all streams of all services
     */
    override fun stopStream() {
        tsServices.forEach {
            removeStreams(it)
        }
    }

    override fun release() {
        tsServices.clear()
    }

    /**
     * Get a new Pid between [0x00010 and 0x1FFE]. Call it when you need a new PID for a PMT, streams,...
     * @return a short value
     */
    private fun getNewPid(): Short {
        val currentPids =
            tsServices.flatMap { it.streams }.map { it.pid } + tsServices.filter { it.pmt != null }
                .map { it.pmt?.pid }

        for (i in TSConst.BASE_PID until 0x1FFA) {
            if (!currentPids.contains(i.toShort())) {
                return i.toShort()
            }
        }

        throw IndexOutOfBoundsException("No empty PID left")
    }

    /**
     * Get PES from stream Pid
     * @param pid stream pid
     * @return PES
     */
    private fun getPes(pid: Short): Pes {
        return tsPes.first { it.stream.pid == pid }
    }

    /**
     * Get list of streams id from a MimeType
     * @param mimeType stream mimetype
     * @return list of streams id with same MimeType
     */
    fun getStreamsId(mimeType: String): List<Short> {
        return getStreams(mimeType).map { it.pid }
    }

    /**
     * Get list of streams from a MimeType
     * @param mimeType stream mimetype
     * @return list of streams with same MimeType
     */
    fun getStreams(mimeType: String): List<Stream> {
        return tsServices.flatMap { it.streams }.filter { it.config.mimeType == mimeType }
    }

    /**
     * Get stream from Pid
     * @param pid stream pid
     * @return streams of Pid
     */
    private fun getStream(pid: Short): Stream {
        return tsServices.flatMap { it.streams }.first { it.pid == pid }
    }

    /**
     * Get Service from ServiceInfo
     * @param tsServiceInfo service info
     * @return Service
     */
    private fun getServices(tsServiceInfo: TsServiceInfo): Service {
        return tsServices.first { it.info == tsServiceInfo }
    }

}