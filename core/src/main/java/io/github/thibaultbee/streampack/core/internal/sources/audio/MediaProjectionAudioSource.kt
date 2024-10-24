/*
 * Copyright (C) 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.internal.sources.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.sources.IMediaProjectionSource

/**
 * The [MediaProjectionAudioSource] class is an implementation of [AudioRecordSource] that
 * captures audio played by other apps.
 *
 * @param context The application context
 * @param enableAcousticEchoCanceler [Boolean.true] to enable AcousticEchoCanceler
 * @param enableNoiseSuppressor [Boolean.true] to enable NoiseSuppressor
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun MediaProjectionAudioSource(
    context: Context,
    enableAcousticEchoCanceler: Boolean = true,
    enableNoiseSuppressor: Boolean = true
) = MediaProjectionAudioSource(
    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager,
    enableAcousticEchoCanceler,
    enableNoiseSuppressor
)

/**
 * The [MediaProjectionAudioSource] class is an implementation of [IAudioSourceInternal] that
 * captures audio played by other apps.
 *
 * @param mediaProjectionManager The media projection manager
 * @param enableAcousticEchoCanceler [Boolean.true] to enable AcousticEchoCanceler
 * @param enableNoiseSuppressor [Boolean.true] to enable NoiseSuppressor
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaProjectionAudioSource(
    private val mediaProjectionManager: MediaProjectionManager,
    private val enableAcousticEchoCanceler: Boolean = true,
    private val enableNoiseSuppressor: Boolean = true
) : AudioRecordSource(enableAcousticEchoCanceler, enableNoiseSuppressor), IMediaProjectionSource {
    private var mediaProjection: MediaProjection? = null

    /**
     * Set the activity result to get the media projection.
     */
    override var activityResult: ActivityResult? = null

    override fun buildAudioRecord(config: AudioConfig, bufferSize: Int): AudioRecord {
        val activityResult = requireNotNull(activityResult) {
            "MediaProjection requires an activity result to be set"
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(
            activityResult.resultCode,
            activityResult.data!!
        )

        val audioFormat = AudioFormat.Builder()
            .setEncoding(config.byteFormat)
            .setSampleRate(config.sampleRate)
            .setChannelMask(config.channelConfig)
            .build()

        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(
                AudioPlaybackCaptureConfiguration.Builder(requireNotNull(mediaProjection))
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .build()
    }

    override fun stopStream() {
        super.stopStream()

        mediaProjection?.stop()
        mediaProjection = null
    }
}