package io.github.thibaultbee.streampack.app.ui.main.usecases

import android.content.Context
import io.github.thibaultbee.streampack.app.data.storage.DataStoreRepository
import io.github.thibaultbee.streampack.core.streamers.DefaultAudioOnlyStreamer
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BuildStreamerUseCase(
    private val context: Context,
    private val dataStoreRepository: DataStoreRepository
) {
    /**
     * Build a new [ICoroutineStreamer] based on audio and video preferences.
     *
     * Only create a new streamer if the previous one is not the same type.
     *
     * @param previousStreamer Previous streamer to check if we need to create a new one.
     */
    operator fun invoke(previousStreamer: ICoroutineStreamer? = null): ICoroutineStreamer {
        val isAudioEnable = runBlocking {
            dataStoreRepository.isAudioEnableFlow.first()
        }
        val isVideoEnable = runBlocking { dataStoreRepository.isVideoEnableFlow.first() }


        if (isVideoEnable) {
            if (previousStreamer !is DefaultCameraStreamer) {
                return DefaultCameraStreamer(context, isAudioEnable)
            } else {
                if ((previousStreamer.audioSource == null) != !isAudioEnable) {
                    return DefaultCameraStreamer(context, isAudioEnable)
                }
            }
        } else {
            if (previousStreamer !is DefaultAudioOnlyStreamer) {
                return DefaultAudioOnlyStreamer(context)
            }
        }

        return previousStreamer
    }
}