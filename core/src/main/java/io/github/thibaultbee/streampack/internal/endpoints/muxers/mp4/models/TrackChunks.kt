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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.AV1CodecConfigurationBox2
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.AV1SampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.AVCConfigurationBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.AVCSampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.ChunkLargeOffsetBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.DataEntryUrlBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.DataInformationBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.DataReferenceBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.ESDSBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.HEVCConfigurationBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.HEVCSampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.MP4AudioSampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.MediaBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.MediaHeaderBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.MediaInformationBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.OpusSampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.OpusSpecificBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.SampleDescriptionBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.SampleSizeBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.SampleTableBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.SampleToChunkBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.SyncSampleBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TimeToSampleBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackExtendsBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackFragmentBaseMediaDecodeTimeBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackFragmentBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackFragmentHeaderBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackHeaderBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.TrackRunBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.VP9SampleEntry
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.VPCodecConfigurationBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.utils.createHandlerBox
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.utils.createTypeMediaHeaderBox
import io.github.thibaultbee.streampack.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.av.audio.opus.OpusCsdParser
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.AudioSpecificConfigDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.ESDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.descriptors.SLConfigDescriptor
import io.github.thibaultbee.streampack.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.av.video.vpx.VPCodecConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.extensions.clone
import io.github.thibaultbee.streampack.internal.utils.extensions.isAnnexB
import io.github.thibaultbee.streampack.internal.utils.extensions.isAvcc
import io.github.thibaultbee.streampack.internal.utils.extensions.removeStartCode
import io.github.thibaultbee.streampack.internal.utils.extensions.resolution
import io.github.thibaultbee.streampack.internal.utils.extensions.startCodeSize
import java.nio.ByteBuffer

/**
 * The content of a single track for a fragment or MOOV+MDAT
 *
 * Chunks are only for MOOV+MDAT otherwise for fragment, they are flatten.
 */
class TrackChunks(
    val track: Track,
    val onNewSample: (ByteBuffer) -> Unit,
) {
    private val chunks = mutableListOf<Chunk>()
    private var frameId = 1

    val isValid: Boolean
        // Needs at least 2 samples because of time difference
        get() = (numOfSamples > 0) && when (track.config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                this.extra.size == 2
            }

            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                this.extra.size == 3
            }

            MediaFormat.MIMETYPE_VIDEO_VP9 -> {
                this.format.isNotEmpty()
            }

            MediaFormat.MIMETYPE_VIDEO_AV1 -> {
                this.extra.size == 1
            }

            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                this.extra.size == 1
            }

            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                /**
                 * According the MediaCodec, there are 3 parameter sets. But on Pixel 4A, there is
                 * only 1. So we are trying to be compatible with both.
                 */
                (this.extra.size == 3) || (this.extra.size == 1)
            }

            else -> throw IllegalArgumentException("Unsupported mimeType ${track.config.mimeType}")
        }

    private val bufferSizeCalculator = { buffer: ByteBuffer ->
        when (track.config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                // Replace start code with size (from Annex B to AVCC)
                4 + buffer.remaining() - buffer.startCodeSize
            }

            else -> {
                buffer.remaining()
            } // Nothing
        }
    }

    val duration: Long
        get() = chunks.sumOf { it.duration } * track.timescale / TimeUtils.TIME_SCALE

    val firstTimestamp: Long
        get() = chunks.minOf { it.firstTimestamp } * track.timescale / TimeUtils.TIME_SCALE

    val dataSize: Int
        get() = chunks.sumOf { it.getDataSize(bufferSizeCalculator) }

    val hasData: Boolean
        get() = dataSize > 0

    private val numOfSamples: Int
        get() = chunks.sumOf { it.numOfSamples }

    private val onlySyncFrame: Boolean
        get() = chunks.all { it.onlySyncFrame }

    private val syncFrameList: List<Int>
        get() = chunks.flatMap { it.syncFrameList }

    private val sampleSizes: List<Int>
        get() = chunks.flatMap { it.getSampleSizes(bufferSizeCalculator) }

    private val extra: List<List<ByteBuffer>>
        get() = chunks.flatMap { it.extra }

    private val format: List<MediaFormat>
        get() = chunks.flatMap { it.format }

    private val sampleDts: List<Long>
        get() = chunks.flatMap { chunk -> chunk.sampleDts.map { it * track.timescale / TimeUtils.TIME_SCALE } }

    private fun createNewChunk() {
        val newChunk = Chunk(chunks.size + 1)
        chunks.add(newChunk)
    }

    fun add(frame: Frame) {
        if (chunks.isEmpty()) {
            createNewChunk()
        }

        val frameCopy =
            frame.copy(rawBuffer = frame.buffer.clone()) // Do not keep mediacodec buffer
        chunks.last().add(frameId, frameCopy)
        frameId++
    }

    fun write() {
        chunks.forEach { chunk ->
            chunk.writeTo { frame ->
                when (track.config.mimeType) {
                    MediaFormat.MIMETYPE_VIDEO_HEVC,
                    MediaFormat.MIMETYPE_VIDEO_AVC -> {
                        if (frame.buffer.isAnnexB) {
                            // Replace start code with size (from Annex B to AVCC)
                            val noStartCodeBuffer = frame.buffer.removeStartCode()
                            val sizeBuffer = ByteBuffer.allocate(4)
                            sizeBuffer.putInt(0, noStartCodeBuffer.remaining())
                            onNewSample(sizeBuffer)
                            onNewSample(noStartCodeBuffer)
                        } else if (frame.buffer.isAvcc) {
                            onNewSample(frame.buffer)
                        } else {
                            throw IllegalArgumentException(
                                "Unsupported buffer format: buffer start with 0x${
                                    frame.buffer.get(
                                        0
                                    ).toString(16)
                                }, 0x${frame.buffer.get(1).toString(16)}, 0x${
                                    frame.buffer.get(2).toString(16)
                                }, 0x${frame.buffer.get(3).toString(16)}"
                            )
                        }
                    }

                    else -> {
                        onNewSample(frame.buffer)
                    } // Nothing
                }
            }
        }
    }

    fun createTrak(firstChunkOffset: Long): TrackBox {
        val tkhd = createTrackHeaderBox(track.config, format.first())
        val mdhd =
            MediaHeaderBox(version = 0, timescale = track.timescale, duration = duration)
        val hdlr = track.config.createHandlerBox()
        val mhd = track.config.createTypeMediaHeaderBox()
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

    private fun createTrackHeaderBox(config: Config, format: MediaFormat): TrackHeaderBox {
        val resolution = when (config) {
            is AudioConfig -> Size(0, 0)
            is VideoConfig -> format.resolution
            else -> throw IllegalArgumentException("Unsupported config")
        }
        val volume = when (config) {
            is AudioConfig -> 1.0f
            else -> 0.0f
        }
        return TrackHeaderBox(
            id = track.id,
            version = 0,
            flags = listOf(
                TrackHeaderBox.TrackFlag.ENABLED,
                TrackHeaderBox.TrackFlag.IN_MOVIE,
                TrackHeaderBox.TrackFlag.IN_PREVIEW
            ),
            duration = duration,
            volume = volume,
            resolution = resolution
        )
    }

    private fun createSampleToChunkBox(): SampleToChunkBox {
        val filteredSampleToChunkEntries = mutableListOf<SampleToChunkBox.Entry>()
        chunks.forEach {
            try {
                val last = filteredSampleToChunkEntries.last()
                if (last.samplesPerChunk != it.numOfSamples) {
                    filteredSampleToChunkEntries.add(
                        SampleToChunkBox.Entry(
                            it.id,
                            it.numOfSamples,
                            1
                        )
                    )
                }
            } catch (e: NoSuchElementException) {
                // First entry
                filteredSampleToChunkEntries.add(
                    SampleToChunkBox.Entry(
                        it.id,
                        it.numOfSamples,
                        1
                    )
                )
            }
        }

        return SampleToChunkBox(filteredSampleToChunkEntries)
    }

    private fun createChunkOffsetBox(firstChunkOffset: Long): ChunkLargeOffsetBox {
        val chunkOffsets = mutableListOf<Long>()
        chunks.map { it.getDataSize(bufferSizeCalculator) }.forEach {
            try {
                chunkOffsets.add(chunkOffsets.last() + it.toLong())
            } catch (e: NoSuchElementException) {
                // First entry
                chunkOffsets.add(firstChunkOffset)
            }
        }
        return ChunkLargeOffsetBox(chunkOffsets)
    }

    private fun createTimeToSampleBox(): TimeToSampleBox {
        return TimeToSampleBox.fromDts(sampleDts, true)
    }

    private fun createSampleDescriptionBox(): SampleDescriptionBox {
        val sampleEntry = when (track.config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                val format = this.format.first()
                val extra = this.extra
                require(extra.size == 2) { "For AVC, extra must contain 2 parameter sets" }
                (track.config as VideoConfig)
                AVCSampleEntry(
                    format.resolution,
                    avcC = AVCConfigurationBox(
                        AVCDecoderConfigurationRecord.fromParameterSets(
                            extra[0],
                            extra[1]
                        )
                    ),
                )
            }

            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                val format = this.format.first()
                val extra = this.extra
                require(extra.size == 3) { "For HEVC, extra must contain 3 parameter sets" }
                (track.config as VideoConfig)
                HEVCSampleEntry(
                    format.resolution,
                    hvcC = HEVCConfigurationBox(
                        HEVCDecoderConfigurationRecord.fromParameterSets(
                            extra.flatten()
                        )
                    ),
                )
            }

            MediaFormat.MIMETYPE_VIDEO_VP9 -> {
                val format = this.format.first()
                (track.config as VideoConfig)
                VP9SampleEntry(
                    format.resolution,
                    vpcC = VPCodecConfigurationBox(
                        VPCodecConfigurationRecord.fromMediaFormat(format)
                    ),
                )
            }

            MediaFormat.MIMETYPE_VIDEO_AV1 -> {
                val format = this.format.first()
                val extra = this.extra
                require(extra.size == 1) { "For AV1, extra must contain 1 extra" }
                (track.config as VideoConfig)
                AV1SampleEntry(
                    format.resolution,
                    av1C = AV1CodecConfigurationBox2(
                        extra[0][0]
                    ),
                )
            }

            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                (track.config as AudioConfig)
                MP4AudioSampleEntry(
                    AudioConfig.getNumberOfChannels(track.config.channelConfig).toShort(),
                    AudioConfig.getNumOfBytesPerSample(track.config.byteFormat).toShort(),
                    track.config.sampleRate,
                    esds = ESDSBox(
                        ESDescriptor(
                            esId = 0,
                            streamPriority = 0,
                            decoderConfigDescriptor = AudioSpecificConfigDescriptor(
                                upStream = false,
                                bufferSize = 1536, //TODO: get from somewhere
                                maxBitrate = track.config.startBitrate,
                                avgBitrate = track.config.startBitrate,
                                extra[0][0]
                            ),
                            slConfigDescriptor = SLConfigDescriptor(predefined = 2)
                        )
                    )
                )
            }

            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                val extra = this.extra
                require((this.extra.size == 3) || (this.extra.size == 1)) { "For Opus, extra must contain 1 or 3 parameter sets" }
                (track.config as AudioConfig)
                val triple = OpusCsdParser.parse(extra[0][0])
                val identificationHeader = triple.first
                OpusSampleEntry(
                    AudioConfig.getNumberOfChannels(track.config.channelConfig).toShort(),
                    dOps = OpusSpecificBox(
                        outputChannelCount = identificationHeader.channelCount,
                        preSkip = identificationHeader.preSkip,
                        inputSampleRate = identificationHeader.inputSampleRate,
                        outputGain = identificationHeader.outputGain,
                        channelMappingFamily = identificationHeader.channelMappingFamily,
                        channelMapping = identificationHeader.channelMapping
                    )
                )
            }

            else -> throw IllegalArgumentException("Unsupported mimeType ${track.config.mimeType}")
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
        return TrackExtendsBox(track.id)
    }

    fun createTraf(baseDataOffset: Long, moofSize: Int): TrackFragmentBox {
        val tfhd = createTrackFragmentHeaderBox(baseDataOffset)
        val tfdt =
            TrackFragmentBaseMediaDecodeTimeBox((firstTimestamp - track.firstTimestamp) * TimeUtils.TIME_SCALE / track.timescale)
        val trun = createTrackRunBox(moofSize)
        return TrackFragmentBox(tfhd, tfdt, trun)
    }

    private fun createTrackFragmentHeaderBox(baseDataOffset: Long): TrackFragmentHeaderBox {
        return TrackFragmentHeaderBox(
            id = track.id,
            baseDataOffset = baseDataOffset,
            defaultSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.OTHERS,
                isNonSyncSample = true
            )
        )
    }

    private fun createTrackRunBox(moofSize: Int): TrackRunBox {
        val sampleDts = sampleDts
        val sampleSizes = sampleSizes

        require(sampleDts.size == sampleSizes.size) { "Samples dts and sizes must have the same size" }

        val lastEntryIndex = sampleSizes.size - 1
        val entries = sampleSizes.mapIndexed { index, size ->
            TrackRunBox.Entry(
                sampleDuration = if (index == lastEntryIndex) {
                    0
                } else {
                    sampleDts[index + 1] - sampleDts[index]
                }.toInt(), sampleSize = size
            )
        }

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