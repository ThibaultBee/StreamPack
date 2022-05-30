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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import android.util.Size
import io.github.thibaultbee.streampack.internal.utils.put
import io.github.thibaultbee.streampack.internal.utils.putFixed1616
import io.github.thibaultbee.streampack.internal.utils.putShort
import io.github.thibaultbee.streampack.internal.utils.putString
import java.nio.ByteBuffer

sealed class SampleEntry(type: String, private val dataReferenceId: Short, val version: Int = 0) :
    Box(type) {
    override val size: Int = super.size + 8

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.put(ByteArray(6))
        buffer.putShort(dataReferenceId)
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

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.put(ByteArray(16)) // pre_defined + reserved + pre_defined
        buffer.putShort(resolution.width)
        buffer.putShort(resolution.height)
        buffer.putFixed1616(horizontalResolution)
        buffer.putFixed1616(verticalResolution)
        buffer.putInt(0) // reserved
        buffer.putShort(frameCount) // reserved
        compressorName?.let {
            buffer.put(it.length)
            buffer.putString(it)
        }
        buffer.put(ByteArray(32 - (compressorName?.let { it.length + 1 /* size */ }
            ?: 0))) // reserved
        buffer.putShort(depth)
        buffer.putShort(-1) // pre_defined
        otherBoxes.forEach { it.write(buffer) }
        clap?.write(buffer)
        pasp?.write(buffer)
    }
}

class AVCSampleEntry(
    resolution: Size,
    horizontalResolution: Int = 72,
    verticalResolution: Int = 72,
    frameCount: Short = 1,
    compressorName: String? = null,
    depth: Short = 0x0018,
    avcc: AVCConfigurationBox,
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
    mutableListOf<Box>(avcc).apply {
        btrt?.let { add(it) }
        addAll(extensionDescriptorsBox)
    },
    clap,
    pasp
)

class MP4AudioSampleEntry(
    channelCount: Short,
    sampleSize: Short,
    sampleRate: Int,
    private val edts: ByteBuffer,
    btrt: BitRateBox? = null,
) :
    AudioSampleEntry(
        "mp4a",
        0,
        channelCount,
        sampleSize,
        sampleRate,
        mutableListOf<Box>().apply { btrt?.let { add(it) } }) {
    override val size: Int = super.size + edts.remaining()

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.put(edts)
    }
}

open class AudioSampleEntry(
    type: String,
    version: Int = 0,
    private val channelCount: Short,
    private val sampleSize: Short,
    private val sampleRate: Int,
    private val otherBoxes: List<Box> = emptyList(),
) : SampleEntry(type, 3, version) {
    override val size: Int = super.size + 20 + otherBoxes.sumOf { it.size }

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.putShort(version)
        buffer.put(ByteArray(6)) // reserved
        buffer.putShort(channelCount)
        buffer.putShort(sampleSize)
        buffer.putInt(0) // pre_defined + reserved
        buffer.putFixed1616(sampleRate)
        otherBoxes.forEach { it.write(buffer) }
    }
}