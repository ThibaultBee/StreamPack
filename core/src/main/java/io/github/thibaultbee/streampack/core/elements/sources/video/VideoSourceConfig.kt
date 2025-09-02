package io.github.thibaultbee.streampack.core.elements.sources.video

import android.util.Size
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.DEFAULT_FPS
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.DEFAULT_RESOLUTION
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile

data class VideoSourceConfig(
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size = DEFAULT_RESOLUTION,

    /**
     * Video framerate.
     */
    val fps: Float = DEFAULT_FPS,

    /**
     * The dynamic range profile.
     *
     * **See Also:** [DynamicRangeProfiles](https://developer.android.com/reference/android/hardware/camera2/params/DynamicRangeProfiles)
     */
    val dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr
) {
    /**
     * The Android dynamic range profile.
     */
    val dynamicRange = dynamicRangeProfile.dynamicRange
}