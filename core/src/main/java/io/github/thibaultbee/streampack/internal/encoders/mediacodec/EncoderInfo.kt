package io.github.thibaultbee.streampack.internal.encoders.mediacodec

import android.media.MediaCodecInfo
import io.github.thibaultbee.streampack.internal.encoders.IEncoderSettings
import io.github.thibaultbee.streampack.internal.utils.extensions.isAudio
import io.github.thibaultbee.streampack.internal.utils.extensions.isVideo

sealed class EncoderInfo(codecInfo: MediaCodecInfo, mimeType: String) :
    IEncoderSettings.IEncoderInfo {
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
    private val videoCapabilities = codecCapabilities.videoCapabilities

    val supportedWidths = videoCapabilities.supportedWidths
    val supportedHeights = videoCapabilities.supportedHeights
    val supportedFrameRates = videoCapabilities.supportedFrameRates
    val supportedBitrates = videoCapabilities.bitrateRange
}

class AudioEncoderInfo(codecInfo: MediaCodecInfo, mimeType: String) :
    EncoderInfo(codecInfo, mimeType) {
    private val audioCapabilities = codecCapabilities.audioCapabilities

    val supportedSampleRates = audioCapabilities.supportedSampleRates
    val supportedBitrates = audioCapabilities.bitrateRange
}