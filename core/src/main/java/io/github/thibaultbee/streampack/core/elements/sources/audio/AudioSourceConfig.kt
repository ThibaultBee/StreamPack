package io.github.thibaultbee.streampack.core.elements.sources.audio

import android.media.AudioFormat
import io.github.thibaultbee.streampack.core.elements.utils.ByteFormatValue
import io.github.thibaultbee.streampack.core.elements.utils.ChannelConfigValue

data class AudioSourceConfig(
    /**
     * Audio capture sample rate in Hz.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): "44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices."
     */
    val sampleRate: Int,

    /**
     * Audio channel configuration.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): " AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices."
     *
     * @see [AudioFormat.CHANNEL_IN_MONO]
     * @see [AudioFormat.CHANNEL_IN_STEREO]
     */
    @ChannelConfigValue val channelConfig: Int,

    /**
     * Audio byte format.
     *
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    @ByteFormatValue val byteFormat: Int
)
