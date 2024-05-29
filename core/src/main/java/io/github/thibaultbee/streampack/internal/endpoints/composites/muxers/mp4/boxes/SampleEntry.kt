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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.boxes

import android.util.Size
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putFixed1616
import io.github.thibaultbee.streampack.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.internal.utils.extensions.putString
import java.nio.ByteBuffer

sealed class SampleEntry(type: String, private val dataReferenceId: Short, val version: Int = 0) :
    Box(type) {
    override val size: Int = super.size + 8

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(ByteArray(6))
        output.putShort(dataReferenceId)
    }
}

open class VisualSampleEntry(
    type: String,
    private val resolution: Size,
    private val horizontalResolution: Int = 72,
    private val verticalResolution: Int = 72,
    private val frameCount: Short = 1,
    private val compressorName: String? = null,
    private val depth: Short = 0x0018,
    private val otherBoxes: List<Box> = emptyList(),
    private val clap: CleanApertureBox? = null,
    private val pasp: PixelAspectRatioBox? = null
) :
    SampleEntry(type, 1) {
    init {
        compressorName?.let { require(it.length < 32) { "compressorName must be less than 32 bytes long" } }
    }

    override val size: Int =
        super.size + 70 + otherBoxes.sumOf { it.size } + (clap?.size ?: 0) + (pasp?.size ?: 0)

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(ByteArray(16)) // pre_defined + reserved + pre_defined
        output.putShort(resolution.width)
        output.putShort(resolution.height)
        output.putFixed1616(horizontalResolution)
        output.putFixed1616(verticalResolution)
        output.putInt(0) // reserved
        output.putShort(frameCount) // reserved
        compressorName?.let {
            output.put(it.length)
            output.putString(it)
        }
        output.put(ByteArray(32 - (compressorName?.let { it.length + 1 /* size */ }
            ?: 0))) // reserved
        output.putShort(depth)
        output.putShort(-1) // pre_defined
        otherBoxes.forEach { it.write(output) }
        clap?.write(output)
        pasp?.write(output)
    }
}

class AVCSampleEntry(
    resolution: Size,
    horizontalResolution: Int = 72,
    verticalResolution: Int = 72,
    frameCount: Short = 1,
    compressorName: String? = "AVC Coding",
    depth: Short = 0x0018,
    avcC: AVCConfigurationBox,
    btrt: BitRateBox? = null,
    extensionDescriptorsBox: List<Box> = emptyList(),
    clap: CleanApertureBox? = null,
    pasp: PixelAspectRatioBox? = null
) : VisualSampleEntry(
    "avc1",
    resolution,
    horizontalResolution,
    verticalResolution,
    frameCount,
    compressorName,
    depth,
    mutableListOf<Box>(avcC).apply {
        btrt?.let { add(it) }
        addAll(extensionDescriptorsBox)
    },
    clap,
    pasp
)

class HEVCSampleEntry(
    resolution: Size,
    horizontalResolution: Int = 72,
    verticalResolution: Int = 72,
    frameCount: Short = 1,
    compressorName: String? = "HEVC Coding",
    depth: Short = 0x0018,
    hvcC: HEVCConfigurationBox,
    btrt: BitRateBox? = null,
    extensionDescriptorsBox: List<Box> = emptyList(),
    clap: CleanApertureBox? = null,
    pasp: PixelAspectRatioBox? = null
) : VisualSampleEntry(
    "hvc1",
    resolution,
    horizontalResolution,
    verticalResolution,
    frameCount,
    compressorName,
    depth,
    mutableListOf<Box>(hvcC).apply {
        btrt?.let { add(it) }
        addAll(extensionDescriptorsBox)
    },
    clap,
    pasp
)

class VP9SampleEntry(
    resolution: Size,
    horizontalResolution: Int = 72,
    verticalResolution: Int = 72,
    frameCount: Short = 1,
    compressorName: String? = "VP9 Coding",
    depth: Short = 0x0018,
    vpcC: VPCodecConfigurationBox,
    btrt: BitRateBox? = null,
    extensionDescriptorsBox: List<Box> = emptyList(),
    clap: CleanApertureBox? = null,
    pasp: PixelAspectRatioBox? = null
) : VisualSampleEntry(
    "vp09",
    resolution,
    horizontalResolution,
    verticalResolution,
    frameCount,
    compressorName,
    depth,
    mutableListOf<Box>(vpcC).apply {
        btrt?.let { add(it) }
        addAll(extensionDescriptorsBox)
    },
    clap,
    pasp
)

class AV1SampleEntry(
    resolution: Size,
    horizontalResolution: Int = 72,
    verticalResolution: Int = 72,
    frameCount: Short = 1,
    compressorName: String? = "AV1 Coding",
    depth: Short = 0x0018,
    av1C: AV1CodecConfigurationBox,
    btrt: BitRateBox? = null,
    extensionDescriptorsBox: List<Box> = emptyList(),
    clap: CleanApertureBox? = null,
    pasp: PixelAspectRatioBox? = null
) : VisualSampleEntry(
    "av01",
    resolution,
    horizontalResolution,
    verticalResolution,
    frameCount,
    compressorName,
    depth,
    mutableListOf<Box>(av1C).apply {
        btrt?.let { add(it) }
        addAll(extensionDescriptorsBox)
    },
    clap,
    pasp
)

class OpusSampleEntry(
    channelCount: Short,
    dOps: OpusSpecificBox,
    btrt: BitRateBox? = null,
) :
    AudioSampleEntry(
        "Opus",
        0,
        channelCount,
        16,
        48000,
        mutableListOf<Box>(dOps).apply {
            btrt?.let { add(it) }
        }
    )

class MP4AudioSampleEntry(
    channelCount: Short,
    sampleSize: Short,
    sampleRate: Int,
    esds: ESDSBox,
    btrt: BitRateBox? = null,
) :
    AudioSampleEntry(
        "mp4a",
        0,
        channelCount,
        sampleSize,
        sampleRate,
        mutableListOf<Box>(esds).apply {
            btrt?.let { add(it) }
        }
    )

open class AudioSampleEntry(
    type: String,
    version: Int = 0,
    private val channelCount: Short,
    private val sampleSize: Short,
    private val sampleRate: Int,
    private val otherBoxes: List<Box> = emptyList(),
) : SampleEntry(type, 1, version) {
    override val size: Int = super.size + 20 + otherBoxes.sumOf { it.size }

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putShort(version)
        output.put(ByteArray(6)) // reserved
        output.putShort(channelCount)
        output.putShort(sampleSize)
        output.putInt(0) // pre_defined + reserved
        output.putInt(sampleRate shl 16)
        otherBoxes.forEach { it.write(output) }
    }
}