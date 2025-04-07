package io.github.thibaultbee.streampack.core.streamers.extensions

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.streamers.IVideoStreamer

/**
 * Sets activity result from [ComponentActivity.registerForActivityResult] callback.
 */
fun IVideoStreamer<*>.setActivityResult(activityResult: ActivityResult) {
    val videoSource = videoSourceFlow.value
    if (videoSource !is IMediaProjectionSource) {
        throw IllegalStateException("Video source must be a MediaProjectionVideoSource")
    }
    videoSource.activityResult = activityResult
    if (this is IWithAudioSource) {
        if (audioSourceFlow.value is IMediaProjectionSource) {
            (audioSourceFlow.value as IMediaProjectionSource).activityResult = activityResult
        }
    }
}