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
package io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.audiofx.AudioEffect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import java.util.UUID

/**
 * The [MediaProjectionAudioSource] class is an implementation of [AudioRecordSource] that
 * captures audio played by other apps.
 *
 * @param context The application context
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun MediaProjectionAudioSource(
    context: Context,
) = MediaProjectionAudioSource(
    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager,
)

/**
 * The [MediaProjectionAudioSource] class is an implementation of [IAudioSourceInternal] that
 * captures audio played by other apps.
 *
 * @param mediaProjectionManager The media projection manager
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class MediaProjectionAudioSource(
    private val mediaProjectionManager: MediaProjectionManager,
) : AudioRecordSource(), IMediaProjectionSource {
    private var mediaProjection: MediaProjection? = null

    /**
     * Set the activity result to get the media projection.
     */
    override var activityResult: ActivityResult? = null

    override fun buildAudioRecord(config: AudioSourceConfig, bufferSize: Int): AudioRecord {
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

@RequiresApi(Build.VERSION_CODES.Q)
class MediaProjectionAudioSourceFactory(
    effects: Set<UUID> = setOf(
        AudioEffect.EFFECT_TYPE_AEC,
        AudioEffect.EFFECT_TYPE_NS
    )
) : AudioRecordSourceFactory(effects) {
    override suspend fun createImpl(context: Context) =
        MediaProjectionAudioSource(context)

    override fun isSourceEquals(source: IAudioSourceInternal?): Boolean {
        return source is MediaProjectionAudioSource
    }
}