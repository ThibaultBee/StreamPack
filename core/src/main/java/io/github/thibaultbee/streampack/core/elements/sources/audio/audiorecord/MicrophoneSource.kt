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

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.AudioSourceValue
import java.util.UUID

/**
 * The [MicrophoneSource] class is an implementation of [AudioRecordSource] that captures audio
 * from the microphone.
 *
 * @param audioSourceType The MediaRecorder.AudioSource constant to use. Defaults to CAMCORDER.
 *                        Common values: CAMCORDER(5), VOICE_COMMUNICATION(7)
 */
internal class MicrophoneSource(val audioSourceType: Int = MediaRecorder.AudioSource.CAMCORDER) : AudioRecordSource() {
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun buildAudioRecord(config: AudioSourceConfig, bufferSize: Int): AudioRecord {
        val audioSource = audioSourceType
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioFormat = AudioFormat.Builder()
                .setEncoding(config.byteFormat)
                .setSampleRate(config.sampleRate)
                .setChannelMask(config.channelConfig)
                .build()

            AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioSource(audioSource)
                .build()
        } else {
            AudioRecord(
                audioSource,
                config.sampleRate,
                config.channelConfig,
                config.byteFormat,
                bufferSize
            )
        }
    }
}

/**
 * A factory to create a [MicrophoneSource].
 *
 * @param audioSourceType The MediaRecorder.AudioSource constant to use. Defaults to CAMCORDER.
 *                        Common values: CAMCORDER(5), VOICE_COMMUNICATION(7)
 * @param effects a set of audio effects to apply to the audio source. Defaults to AEC+NS.
 */
class MicrophoneSourceFactory(
    private val audioSourceType: Int = MediaRecorder.AudioSource.CAMCORDER,
    effects: Set<UUID> = defaultAudioEffects
) :
    AudioRecordSourceFactory(effects) {
    
    override suspend fun createImpl(context: Context) = MicrophoneSource(audioSourceType)

    override fun isSourceEquals(source: IAudioSourceInternal?): Boolean {
        // Always return false to force recreation when effects or audio source type changes.
        // Since we can't query the current effects from an existing source, we recreate
        // the source to ensure the new effects configuration is applied.
        return false
    }

    override fun toString(): String {
        return "MicrophoneSourceFactory(audioSourceType=$audioSourceType, effects=$effects)"
    }
}
