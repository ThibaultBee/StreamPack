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
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleDependsOn
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.Samples
import io.github.thibaultbee.streampack.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.av.video.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.clone
import java.nio.ByteBuffer

class MP4Track(
    val id: Int,
    val config: Config,
    var onNewSample: (ByteBuffer) -> Unit
) {
    private var firstTimestamp: Long = 0
    private var _totalDuration: Long = 0
    val dataSize: Int
        get() = currentSamples.size
    val totalDuration: Long
        get() = _totalDuration
    val timescale = TimeUtils.TIME_SCALE

    private var frameId: Int = 1

    private var onlySyncFrame = true
    private val syncFrameList = mutableListOf<Int>()
    private val extradata = mutableListOf<List<ByteBuffer>>()

    private val sampleDts = mutableListOf<Long>()

    private val sampleSizes = mutableListOf<Int>()

    private val chunkSizes = mutableListOf<Int>()

    private var currentSamples = Samples(firstChunk = 1, sampleDescriptionId = 1)
    private val sampleToChunks = mutableListOf<SampleToChunkBox.Entry>()
    private lateinit var currentSampleToChunks: SampleToChunkBox.Entry

    init {
        require(id != 0) { "id must be greater than 0" }
    }

    fun add(frame: Frame) {
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

        // New chunk
        if (currentSamples.size == 0) {
            currentSampleToChunks = SampleToChunkBox.Entry(
                currentSamples.firstChunk,
                0,
                currentSamples.sampleDescriptionId
            )
            sampleToChunks.add(
                currentSampleToChunks
            )
        }

        frame.buffer = frame.buffer.clone() // Do not keep mediacodec buffer
        currentSamples.add(frame)
        currentSampleToChunks.samplesPerChunk++
    }

    fun write() {
        if (currentSamples.size > 0) {
            currentSamples.samples.forEach { onNewSample(it.buffer) }
            chunkSizes.add(currentSamples.size)

            currentSamples = Samples(
                firstChunk = currentSamples.firstChunk + 1,
                sampleDescriptionId = currentSamples.sampleDescriptionId
            )
        }
    }

    fun createTrak(firstChunkOffset: Int): TrackBox {
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
        val co = createChunkOffsetBox(firstChunkOffset)

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
        val filteredSampleToChunkEntries = mutableListOf<SampleToChunkBox.Entry>()
        sampleToChunks.forEach {
            try {
                val last = filteredSampleToChunkEntries.last()
                if ((last.samplesPerChunk != it.samplesPerChunk)
                    || (last.sampleDescriptionId != it.sampleDescriptionId)
                ) {
                    filteredSampleToChunkEntries.add(it)
                }
            } catch (e: NoSuchElementException) {
                filteredSampleToChunkEntries.add(it)
            }
        }

        return SampleToChunkBox(filteredSampleToChunkEntries)
    }

    private fun createChunkOffsetBox(firstChunkOffset: Int): ChunkLargeOffsetBox {
        val chunkOffsets = mutableListOf(firstChunkOffset.toLong())
        chunkSizes.forEach {
            chunkOffsets.add(chunkOffsets.last() + it.toLong())
        }
        return ChunkLargeOffsetBox(chunkOffsets)
    }

    private fun createTimeToSampleBox(): TimeToSampleBox {
        val sampleDurations = mutableListOf<Int>()
        sampleDts.forEachIndexed { index, dts ->
            if (index != 0) {
                sampleDurations.add((dts - sampleDts[index - 1]).toInt())
            }
        }
        val filteredTimeToSampleEntries = mutableListOf<TimeToSampleBox.Entry>()
        var count = 1
        sampleDurations.forEach { duration ->
            try {
                val last = filteredTimeToSampleEntries.last()
                if (duration != last.delta) {
                    filteredTimeToSampleEntries.add(TimeToSampleBox.Entry(count, duration))
                    count++
                }
            } catch (e: NoSuchElementException) {
                filteredTimeToSampleEntries.add(TimeToSampleBox.Entry(count, duration))
                count++
            }
        }

        return TimeToSampleBox(filteredTimeToSampleEntries)
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

    fun createTref(): TrackExtendsBox {
        return TrackExtendsBox(id)
    }

    fun createTraf(baseDataOffset: Long, moofSize: Int): TrackFragmentBox {
        val tfhd = createTrackFragmentHeaderBox(baseDataOffset)
        val tfdt = null //TrackFragmentBaseMediaDecodeTimeBox(0)
        val trun = createTrackRunBox(moofSize)
        return TrackFragmentBox(tfhd, tfdt, trun)
    }

    private fun createTrackFragmentHeaderBox(baseDataOffset: Long): TrackFragmentHeaderBox {
        return TrackFragmentHeaderBox(
            id = id,
            baseDataOffset = baseDataOffset,
            defaultSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.OTHERS,
                isNonSyncSample = true
            )
        )
    }

    private fun createTrackRunBox(moofSize: Int): TrackRunBox {
        val entries = currentSamples.samples.map { TrackRunBox.Entry(size = it.buffer.remaining()) }
        return TrackRunBox(
            version = 0,
            dataOffset = moofSize,
            firstSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.NO_OTHER,
                isNonSyncSample = false,
            ),
            entries = entries
        )
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
