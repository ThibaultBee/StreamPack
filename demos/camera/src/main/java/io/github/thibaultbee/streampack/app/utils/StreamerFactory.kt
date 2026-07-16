package io.github.thibaultbee.streampack.app.utils

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.app.data.rotation.RotationRepository
import io.github.thibaultbee.streampack.app.data.storage.DataStoreRepository
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.streamers.single.IAudioSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoOnlySingleStreamer
import kotlinx.coroutines.flow.first

class StreamerFactory(
    private val application: Application,
    private val storageRepository: DataStoreRepository,
    private val rotationRepository: RotationRepository
) {
    suspend fun build(withAudio: Boolean): IVideoSingleStreamer {
        val streamer = if (withAudio) {
            SingleStreamer(application, audioInputMode = StreamerPipeline.AudioInputMode.PUSH)
        } else {
            VideoOnlySingleStreamer(application)
        }

        streamer.setTargetRotation(rotationRepository.rotationFlow.first())

        if (ActivityCompat.checkSelfPermission(
                application, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            streamer.setVideoSource(CameraSourceFactory(application.defaultCameraId))
        }

        storageRepository.videoConfigFlow.first()?.let {
            streamer.setVideoConfig(it)
        }

        if (ActivityCompat.checkSelfPermission(
                application, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val audioStreamer = streamer as? IAudioSingleStreamer
            audioStreamer?.let {
                it.setAudioSource(MicrophoneSourceFactory())
                storageRepository.audioConfigFlow.first()?.let { config ->
                    it.setAudioConfig(config)
                }
            }
        }

        return streamer
    }
}
