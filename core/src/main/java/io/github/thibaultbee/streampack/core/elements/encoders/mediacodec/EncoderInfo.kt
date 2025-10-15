package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import android.media.MediaCodecInfo
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isAudio
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isVideo

sealed class EncoderInfo(codecInfo: MediaCodecInfo, mimeType: String) : IEncoder.IEncoderInfo {
    protected val codecCapabilities: MediaCodecInfo.CodecCapabilities =
        codecInfo.getCapabilitiesForType(mimeType)

    override val name = codecInfo.name

    companion object {
        fun build(codecInfo: MediaCodecInfo, mimeType: String): EncoderInfo {
            return if (codecInfo.isEncoder) {
                when {
                    mimeType.isVideo -> VideoEncoderInfo(codecInfo, mimeType)
                    mimeType.isAudio -> AudioEncoderInfo(codecInfo, mimeType)
                    else -> throw IllegalArgumentException("Unsupported encoder type: $mimeType")
                }
            } else {
                throw IllegalArgumentException("MediaCodecInfo ${codecInfo.name} is not an encoder for $mimeType")
            }
        }
    }
}

class VideoEncoderInfo(codecInfo: MediaCodecInfo, mimeType: String) :
    EncoderInfo(codecInfo, mimeType) {
    private val videoCapabilities = requireNotNull(codecCapabilities.videoCapabilities) {
        "Video capabilities are null for encoder $name and mime type $mimeType"
    }

    val supportedWidths = videoCapabilities.supportedWidths
    val supportedHeights = videoCapabilities.supportedHeights
    val supportedFrameRates = videoCapabilities.supportedFrameRates
    val supportedBitrates = videoCapabilities.bitrateRange
}

class AudioEncoderInfo(codecInfo: MediaCodecInfo, mimeType: String) :
    EncoderInfo(codecInfo, mimeType) {
    private val audioCapabilities = requireNotNull(codecCapabilities.audioCapabilities) {
        "Audio capabilities are null for encoder $name and mime type $mimeType"
    }

    val supportedSampleRates = audioCapabilities.supportedSampleRates
    val supportedBitrates = audioCapabilities.bitrateRange
}