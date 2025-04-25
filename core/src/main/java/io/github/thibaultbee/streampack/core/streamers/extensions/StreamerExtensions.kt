package io.github.thibaultbee.streampack.core.streamers.extensions

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource

/**
 * Sets the activity result for the video [IMediaProjectionSource].
 *
 * This function is used to set the activity result from the [ComponentActivity.registerForActivityResult] callback.
 *
 * @param activityResult The activity result to set.
 */
fun IWithVideoSource.setVideoActivityResult(activityResult: ActivityResult) {
    val videoSource = videoSourceFlow.value
    if (videoSource !is IMediaProjectionSource) {
        throw IllegalStateException("Video source must be a MediaProjectionVideoSource")
    }
    videoSource.activityResult = activityResult
}


/**
 * Sets the activity result for the video [IMediaProjectionSource].
 *
 * This function is used to set the activity result from the [ComponentActivity.registerForActivityResult] callback.
 *
 * @param activityResult The activity result to set.
 */
fun IWithAudioSource.setAudioActivityResult(activityResult: ActivityResult) {
    val audioSource = audioSourceFlow.value
    if (audioSource !is IMediaProjectionSource) {
        throw IllegalStateException("Audio source must be a MediaProjectionAudioSource")
    }
    audioSource.activityResult = activityResult
}

