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
 * @param audioSource The audio source to use (e.g., MediaRecorder.AudioSource.MIC).
 */
internal class MicrophoneSource(@AudioSourceValue val audioSource: Int) :
    AudioRecordSource() {
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun buildAudioRecord(config: AudioSourceConfig, bufferSize: Int): AudioRecord {
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
 * @param audioSource the audio source to use (e.g., MediaRecorder.AudioSource.MIC)
 * @param effects a set of audio effects to apply to the audio source
 */
class MicrophoneSourceFactory(
    @AudioSourceValue val audioSource: Int = MediaRecorder.AudioSource.CAMCORDER,
    effects: Set<UUID> = defaultAudioEffects
) :
    AudioRecordSourceFactory(effects) {
    override suspend fun createImpl(context: Context) = MicrophoneSource(audioSource)

    override fun isSourceEquals(source: IAudioSourceInternal?): Boolean {
        return source is MicrophoneSource && this.audioSource == source.audioSource
    }

    override fun toString(): String {
        return "MicrophoneSourceFactory(audioSource=$audioSource, effects=$effects)"
    }
}