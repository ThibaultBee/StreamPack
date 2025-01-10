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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AudioEffect
import android.os.Build
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig

/**
 * The [MicrophoneSource] class is an implementation of [AudioRecordSource] that captures audio
 * from the microphone.
 */
class MicrophoneSource : AudioRecordSource() {
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
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .build()
        } else {
            AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                config.sampleRate,
                config.channelConfig,
                config.byteFormat,
                bufferSize
            )
        }
    }

    companion object {
        /**
         * Build a [MicrophoneSource] with default effects.
         */
        fun buildDefaultMicrophoneSource(): MicrophoneSource {
            return MicrophoneSource().apply {
                if (isEffectAvailable(AudioEffect.EFFECT_TYPE_AEC)) {
                    addEffect(AudioEffect.EFFECT_TYPE_AEC)
                }
                if (isEffectAvailable(AudioEffect.EFFECT_TYPE_NS)) {
                    addEffect(AudioEffect.EFFECT_TYPE_NS)
                }
            }
        }
    }
}