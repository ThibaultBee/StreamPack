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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toInt
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer

/**
 * aligned (8) class VPCodecConfigurationRecord {
 *     unsigned int (8)     profile;
 *     unsigned int (8)     level;
 *     unsigned int (4)     bitDepth;
 *     unsigned int (3)     chromaSubsampling;
 *     unsigned int (1)     videoFullRangeFlag;
 *     unsigned int (8)     colourPrimaries;
 *     unsigned int (8)     transferCharacteristics;
 *     unsigned int (8)     matrixCoefficients;
 *     unsigned int (16)    codecIntializationDataSize;
 *     unsigned int (8)[]   codecIntializationData;
 * }
 */
data class VPCodecConfigurationRecord(
    private val profile: Profile,
    private val level: Level,
    private val bitDepth: Byte,
    private val chromaSubsampling: ChromaSubsampling,
    private val videoFullRangeFlag: Boolean,
    private val colorPrimaries: ColorPrimaries,
    private val transferCharacteristics: TransferCharacteristics,
    private val matrixCoefficients: MatrixCoefficients,
    private val codecInitializationData: ByteBuffer? = null,
) : ByteBufferWriter() {
    override val size: Int = getSize() + (codecInitializationData?.remaining() ?: 0)

    init {
        if (matrixCoefficients == MatrixCoefficients.IDENTITY) {
            require(chromaSubsampling == ChromaSubsampling.YUV444) { "If matrixCoefficients is 0 (RGB), then chroma subsampling MUST be 3 (4:4:4)." }
        }
    }

    override fun write(output: ByteBuffer) {
        output.put(profile.value)
        output.put(level.value)
        output.put((bitDepth shl 3) or (chromaSubsampling.value shl 1) or (videoFullRangeFlag.toInt()))
        output.put(colorPrimaries.value)
        output.put(transferCharacteristics.value)
        output.put(matrixCoefficients.value)
        if (codecInitializationData == null) {
            output.putShort(0) // codecIntializationDataSize
        } else {
            output.putShort(codecInitializationData.remaining()) // codecIntializationDataSize
            output.put(codecInitializationData)
        }
    }

    companion object {
        private const val TAG = "VPCodecConfigurationRecord"

        private const val VP_DECODER_CONFIGURATION_RECORD_SIZE = 8

        /**
         * Creates a [VPCodecConfigurationRecord] from a [MediaFormat] object.
         *
         * VP9 [MediaFormat] example:
         * ```
         * {max-bitrate=1138000, crop-right=359, level=128, mime=video/x-vnd.on2.vp9,
         * profile=1, bitrate=1138000, intra-refresh-period=0, color-standard=4, color-transfer=3,
         * crop-bottom=639, video-qp-average=0, crop-left=0, width=360, bitrate-mode=1,
         * color-range=2, crop-top=0, frame-rate=30, height=640}
         * ```
         */
        fun fromMediaFormat(format: MediaFormat): VPCodecConfigurationRecord {
            val rawProfile = format.getInteger(MediaFormat.KEY_PROFILE)
            val profile = Profile.fromMediaFormat(rawProfile)

            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Level.fromMediaFormat(format.getInteger(MediaFormat.KEY_LEVEL))
            } else {
                Level.UNDEFINED // 0 is undefined
            }

            val mimeType = format.getString(MediaFormat.KEY_MIME)!!
            // field is 8 or 10 (12 bits not supported on Android)
            val bitDepth = if (DynamicRangeProfile.fromProfile(
                    mimeType,
                    rawProfile
                ).isHdr
            ) {
                10
            } else {
                8
            }

            val chromaSubsampling = try {
                ChromaSubsampling.fromValue(
                    format.getInteger(MediaFormat.KEY_COLOR_FORMAT).toByte()
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Missing KEY_COLOR_FORMAT in MediaFormat")
                ChromaSubsampling.YUV420_VERTICAL
            }

            val colorRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                format.getInteger(MediaFormat.KEY_COLOR_RANGE)
            } else {
                0 // 0 is undefined
            }
            val videoFullRangeFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                colorRange == MediaFormat.COLOR_RANGE_FULL
            } else {
                false
            }

            val colorStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
            } else {
                null
            }

            val colorPrimaries = if (colorStandard != null) {
                ColorPrimaries.fromColorStandard(colorStandard)
            } else {
                ColorPrimaries.UNSPECIFIED
            }

            val transferCharacteristics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TransferCharacteristics.fromColorTransfer(format.getInteger(MediaFormat.KEY_COLOR_TRANSFER))
            } else {
                TransferCharacteristics.UNSPECIFIED
            }

            val matrixCoefficients = if (colorStandard != null) {
                MatrixCoefficients.fromColorStandard(colorStandard)
            } else {
                MatrixCoefficients.UNSPECIFIED
            }
            return VPCodecConfigurationRecord(
                profile = profile,
                level = level,
                bitDepth = bitDepth.toByte(),
                chromaSubsampling = chromaSubsampling,
                videoFullRangeFlag = videoFullRangeFlag,
                colorPrimaries = colorPrimaries,
                transferCharacteristics = transferCharacteristics,
                matrixCoefficients = matrixCoefficients,
                codecInitializationData = null // Always null for VP8 and VP9
            )
        }

        fun getSize(): Int {
            val size =
                VP_DECODER_CONFIGURATION_RECORD_SIZE

            return size
        }
    }
}