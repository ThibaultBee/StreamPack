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
package io.github.thibaultbee.streampack.internal.muxers.mp4

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.muxers.mp4.boxes.*
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.Chunk
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.DecodingTime
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleToChunk
import io.github.thibaultbee.streampack.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.av.video.AVCDecoderConfigurationRecord
import java.nio.ByteBuffer

class MP4Track(
    val id: Int,
    val config: Config,
    private val numOfSamplePerChunk: Int,
    var onNewChunk: (ByteBuffer) -> Unit
) {
    private var firstTimestamp: Long = 0
    private var _totalDuration: Long = 0
    val totalDuration: Long
        get() = _totalDuration
    val timescale = TimeUtils.TIME_SCALE

    private var frameId: Int = 1

    private var onlySyncFrame = true
    private val syncFrameList = mutableListOf<Int>()
    private val extradata = mutableListOf<List<ByteBuffer>>()

    private val sampleDts = mutableListOf<Long>()

    private val sampleSizes = mutableListOf<Int>()

    private val chunkOffsets = mutableListOf<Long>()

    private var currentChunk = Chunk(firstChunk = 1, sampleDescriptionId = 1)
    private val sampleToChunks = mutableListOf<SampleToChunk>()

    init {
        require(id != 0) { "id must be greater than 0" }
    }

    fun write(frame: Frame, frameOffset: Long) {
        frame.extra?.let {
            extradata.add(it)
        }

        if (frame.isKeyFrame) {
            syncFrameList.add(frameId)
        } else {
            onlySyncFrame = false
        }
        frameId++

        if (firstTimestamp == 0L) {
            firstTimestamp = frame.pts
        }
        _totalDuration = (frame.pts - firstTimestamp) * timescale / TimeUtils.TIME_SCALE

        frame.dts?.let {
            sampleDts.add(it)
        } ?: sampleDts.add(frame.pts)

        sampleSizes.add(frame.buffer.remaining())

        if (currentChunk.size == numOfSamplePerChunk) {
            writeCurrentChunk()
            chunkOffsets.add(frameOffset)
            currentChunk = Chunk(
                firstChunk = currentChunk.firstChunk + 1,
                sampleDescriptionId = currentChunk.sampleDescriptionId
            )
        } else {
            currentChunk.add(frame)
        }
    }

    private fun writeCurrentChunk() {
        sampleToChunks.add(currentChunk.toSampleToChunk())
        val byteBuffer = currentChunk.write()
        onNewChunk(byteBuffer)
    }

    fun writeLastChunk() {
        if (currentChunk.size > 0) {
            writeCurrentChunk()
        }
    }

    fun getTrak(): TrackBox {
        val tkhd = createTrackHeaderBox(config)
        val mdhd =
            MediaHeaderBox(version = 0, timescale = timescale, duration = totalDuration)
        val hdlr = config.createHandlerBox()
        val mhd = config.createTypeMediaHeaderBox()
        val dinf = DataInformationBox(DataReferenceBox(DataEntryUrlBox()))
        val stsd = createSampleDescriptionBox()
        val stts = createTimeToSampleBox()
        val stss = createSyncSampleBox()
        val stsc = createSampleToChunkBox()
        val stsz = SampleSizeBox(sampleSizeEntries = sampleSizes)
        val co = ChunkLargeOffsetBox(chunkOffsets)

        val stbl = SampleTableBox(stsd, stts, stss, stsc, stsz, co)
        val minf = MediaInformationBox(mhd, dinf, stbl)
        val mdia = MediaBox(mdhd, hdlr, minf)
        return TrackBox(tkhd, mdia)
    }

    private fun createTrackHeaderBox(config: Config): TrackHeaderBox {
        val resolution = when (config) {
            is AudioConfig -> Size(0, 0)
            is VideoConfig -> config.resolution
            else -> throw IllegalArgumentException("Unsupported config")
        }
        val volume = when (config) {
            is AudioConfig -> 1.0f
            else -> 0.0f
        }
        return TrackHeaderBox(
            id = id,
            version = 0,
            flags = listOf(
                TrackHeaderBox.TrackFlag.ENABLED,
                TrackHeaderBox.TrackFlag.IN_MOVIE,
                TrackHeaderBox.TrackFlag.IN_PREVIEW
            ),
            duration = totalDuration,
            volume = volume,
            resolution = resolution
        )
    }

    private fun createSampleToChunkBox(): SampleToChunkBox {
        val sampleToChunkEntries = mutableListOf<SampleToChunk>()
        sampleToChunks.forEachIndexed { index, sampleToChunk ->
            if (index != 0) {
                val previousSampleToChunk = sampleToChunks[index - 1]
                if ((sampleToChunk.samplesPerChunk != previousSampleToChunk.samplesPerChunk)
                    || (sampleToChunk.sampleDescriptionId != previousSampleToChunk.sampleDescriptionId)
                ) {
                    sampleToChunkEntries.add(sampleToChunk)
                }
            }
        }
        return SampleToChunkBox(sampleToChunkEntries)
    }

    private fun createTimeToSampleBox(): TimeToSampleBox {
        val sampleDurations = mutableListOf<Int>()
        sampleDts.forEachIndexed { index, dts ->
            if (index != 0) {
                sampleDurations.add((dts - sampleDts[index - 1]).toInt())
            }
        }
        val sampleEntries = mutableListOf<DecodingTime>()
        var count = 1
        sampleDurations.forEachIndexed { index, duration ->
            if (index != 0) {
                val previousDuration = sampleDurations[index - 1]
                if (duration == previousDuration) {
                    count++
                } else {
                    sampleEntries.add(DecodingTime(count, previousDuration))
                    count = 1
                }
            }
        }
        return TimeToSampleBox(sampleEntries)
    }

    private fun createSampleDescriptionBox(): SampleDescriptionBox {
        val sampleEntry = when (config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                (config as VideoConfig)
                AVCSampleEntry(
                    config.resolution,
                    avcc = AVCConfigurationBox(
                        AVCDecoderConfigurationRecord.fromSPSAndPPS(
                            extradata[0][0],
                            extradata[0][1]
                        )
                    ),
                )
            }
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                (config as AudioConfig)
                MP4AudioSampleEntry(
                    AudioConfig.getNumberOfChannels(config.channelConfig).toShort(),
                    AudioConfig.getNumOfBytesPerSample(config.byteFormat).toShort(),
                    config.sampleRate,
                    edts = extradata[0][0]
                )
            }
            else -> throw IllegalArgumentException("Unsupported mimeType")
        }
        return SampleDescriptionBox(sampleEntry)
    }

    private fun createSyncSampleBox(): SyncSampleBox? {
        return if (!onlySyncFrame) {
            SyncSampleBox(syncFrameList)
        } else {
            null
        }
    }
}

private fun Config.createTypeMediaHeaderBox(): TypeMediaHeaderBox {
    return when (this) {
        is AudioConfig -> SoundMediaHeaderBox()
        is VideoConfig -> VideoMediaHeaderBox()
        else -> throw IllegalArgumentException("Unsupported config")
    }
}

private fun Config.createHandlerBox(): HandlerBox {
    return when (this) {
        is AudioConfig -> HandlerBox(HandlerBox.HandlerType.SOUND, "SoundHandler")
        is VideoConfig -> HandlerBox(HandlerBox.HandlerType.VIDEO, "VideoHandler")
        else -> throw IllegalArgumentException("Unsupported config")
    }
}
