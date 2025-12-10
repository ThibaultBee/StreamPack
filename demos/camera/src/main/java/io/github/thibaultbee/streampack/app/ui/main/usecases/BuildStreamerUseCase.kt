package io.github.thibaultbee.streampack.app.ui.main.usecases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer

class BuildStreamerUseCase(
    private val context: Context,
) {
    /**
     * Build a new [SingleStreamer] based on audio and video preferences.
     *
     * Only create a new streamer if the previous one is not the same type.
     *
     * It releases the previous streamer if a new one is created.
     *
     * @param previousStreamer Previous streamer to check if we need to create a new one.
     */
    suspend operator fun invoke(
        previousStreamer: SingleStreamer,
        isAudioEnable: Boolean
    ): SingleStreamer {
        if (previousStreamer.withAudio != isAudioEnable) {
            return SingleStreamer(context, isAudioEnable).apply {
                // Get previous streamer config if any
                val audioConfig = previousStreamer.audioConfigFlow.value
                val videoConfig = previousStreamer.videoConfigFlow.value
                if ((audioConfig != null && isAudioEnable)) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        setAudioConfig(audioConfig)
                    }
                }
                if (videoConfig != null) {
                    setVideoConfig(videoConfig)
                }
                previousStreamer.release()
            }
        }
        return previousStreamer
    }
}