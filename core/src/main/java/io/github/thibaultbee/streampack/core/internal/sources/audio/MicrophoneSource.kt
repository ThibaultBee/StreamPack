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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import io.github.thibaultbee.streampack.core.data.AudioConfig

/**
 * The [MicrophoneSource] class is an implementation of [AudioRecordSource] that captures audio
 * from the microphone.
 *
 * @param enableAcousticEchoCanceler [Boolean.true] to enable AcousticEchoCanceler
 * @param enableNoiseSuppressor [Boolean.true] to enable NoiseSuppressor
 */
class MicrophoneSource(
    private val enableAcousticEchoCanceler: Boolean = true,
    private val enableNoiseSuppressor: Boolean = true
) : AudioRecordSource(enableAcousticEchoCanceler, enableNoiseSuppressor) {
    override fun buildAudioRecord(config: AudioConfig, bufferSize: Int): AudioRecord {
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
}