package io.github.thibaultbee.streampack.app.ui.main.usecases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.core.streamers.single.IAudioSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoOnlySingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.withAudio

class BuildStreamerUseCase(
    private val context: Context
) {
    /**
     * Build a new [IVideoSingleStreamer] based on audio and video preferences.
     *
     * Only create a new streamer if the previous one is not the same type.
     *
     * It releases the previous streamer if a new one is created.
     *
     * @param previousStreamer Previous streamer to check if we need to create a new one.
     */
    suspend operator fun invoke(
        previousStreamer: IVideoSingleStreamer,
        isAudioEnable: Boolean
    ): IVideoSingleStreamer {
        if (previousStreamer.withAudio != isAudioEnable) {
            previousStreamer.release()

            val streamer = if (isAudioEnable) {
                SingleStreamer(context)
            } else {
                VideoOnlySingleStreamer(context)
            }

            // Get previous streamer config if any
            if ((previousStreamer is IAudioSingleStreamer) && (streamer is IAudioSingleStreamer)) {
                val audioConfig = previousStreamer.audioConfigFlow.value
                if (audioConfig != null) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        streamer.setAudioConfig(audioConfig)
                    }
                }
            }

            val videoConfig = previousStreamer.videoConfigFlow.value
            if (videoConfig != null) {
                streamer.setVideoConfig(videoConfig)
            }
            return streamer
        }
        return previousStreamer
    }
}