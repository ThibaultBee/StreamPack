package io.github.thibaultbee.streampack.app.ui.main.usecases

import android.content.Context
import io.github.thibaultbee.streampack.app.data.storage.DataStoreRepository
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BuildStreamerUseCase(
    private val context: Context,
    private val dataStoreRepository: DataStoreRepository
) {
    /**
     * Build a new [SingleStreamer] based on audio and video preferences.
     *
     * Only create a new streamer if the previous one is not the same type.
     *
     * @param previousStreamer Previous streamer to check if we need to create a new one.
     */
    operator fun invoke(previousStreamer: SingleStreamer? = null): SingleStreamer {
        val isAudioEnable = runBlocking {
            dataStoreRepository.isAudioEnableFlow.first()
        }

        if (previousStreamer == null) {
            return SingleStreamer(context, isAudioEnable)
        } else if ((previousStreamer.audioInput?.sourceFlow?.value == null) != !isAudioEnable) {
            return SingleStreamer(context, isAudioEnable)
        }
        return previousStreamer
    }
}