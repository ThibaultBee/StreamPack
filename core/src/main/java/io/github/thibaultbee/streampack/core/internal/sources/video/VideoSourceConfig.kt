package io.github.thibaultbee.streampack.core.internal.sources.video

import android.util.Size

data class VideoSourceConfig(
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size,

    /**
     * Video framerate.
     */
    val fps: Int,

    /**
     * The dynamic range profile.
     *
     * **See Also:** [DynamicRangeProfiles](https://developer.android.com/reference/android/hardware/camera2/params/DynamicRangeProfiles)
     */
    val dynamicRangeProfile: Long
)