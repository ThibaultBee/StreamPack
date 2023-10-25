/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.internal.sources

import android.Manifest
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.utils.TAG
import java.nio.ByteBuffer

class AudioCapture : IAudioCapture {
    private var audioRecord: AudioRecord? = null
    private var mutedByteArray: ByteArray? = null

    override var isMuted: Boolean = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(config: AudioConfig) {
        val bufferSize = AudioRecord.getMinBufferSize(
            config.sampleRate,
            config.channelConfig,
            config.byteFormat
        )

        if (bufferSize <= 0) {
            throw IllegalArgumentException(audioRecordErrorToString(bufferSize))
        }

        /**
         * Initialized mutedByteArray with bufferSize. The read buffer length may be different
         * from bufferSize. In this case, mutedByteArray will be resized.
         */
        mutedByteArray = ByteArray(bufferSize)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, config.sampleRate,
            config.channelConfig, config.byteFormat, bufferSize
        ).also {
            if (config.enableEchoCanceler) {
                if (AcousticEchoCanceler.isAvailable()) {
                    AcousticEchoCanceler.create(it.audioSessionId).enabled = true
                } else {
                    Logger.e(TAG, "Acoustic echo canceler is not available")
                }
            }
            if (config.enableNoiseSuppressor) {
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(it.audioSessionId).enabled = true
                } else {
                    Logger.e(TAG, "Noise suppressor is not available")
                }
            }
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalArgumentException("Failed to initialized AudioRecord")
        }
    }

    override fun startStream() {
        audioRecord?.let {
            it.startRecording()

            if (!isRunning()) {
                throw IllegalStateException("AudioCapture: failed to start recording")
            }
        } ?: throw IllegalStateException("AudioCapture: run: : No audioRecorder")
    }

    private fun isRunning() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    override fun stopStream() {
        if (!isRunning()) {
            Logger.d(TAG, "Not running")
            return
        }

        // Stop audio record
        audioRecord?.stop()
    }

    override fun release() {
        mutedByteArray = null
        // Release audio record
        audioRecord?.release()
        audioRecord = null
    }

    private fun getTimestamp(audioRecord: AudioRecord): Long {
        // Get timestamp from AudioRecord
        // If we can not get timestamp through getTimestamp, we timestamp audio sample.
        val timestampOut = AudioTimestamp()
        var timestamp: Long = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (audioRecord.getTimestamp(
                    timestampOut,
                    AudioTimestamp.TIMEBASE_MONOTONIC
                ) == AudioRecord.SUCCESS
            ) {
                timestamp = timestampOut.nanoTime / 1000 // to us
            }
        }
        // Fallback
        if (timestamp < 0) {
            timestamp = TimeUtils.currentTime()
        }

        return timestamp
    }

    override fun getFrame(buffer: ByteBuffer): Frame {
        audioRecord?.let {
            val length = it.read(buffer, buffer.remaining())
            if (length >= 0) {
                return if (isMuted) {
                    if (length != mutedByteArray?.size) {
                        mutedByteArray = ByteArray(length)
                    }
                    buffer.put(mutedByteArray!!, 0, length)
                    buffer.clear()
                    Frame(buffer, getTimestamp(it), format = format)
                } else {
                    Frame(buffer, getTimestamp(it), format = format)
                }
            } else {
                throw IllegalArgumentException(audioRecordErrorToString(length))
            }
        } ?: throw IllegalStateException("AudioCapture: getFrame: No audioRecorder")
    }

    private fun audioRecordErrorToString(audioRecordError: Int) = when (audioRecordError) {
        AudioRecord.ERROR_INVALID_OPERATION -> "AudioRecord returns an invalid operation error"
        AudioRecord.ERROR_BAD_VALUE -> "AudioRecord returns a bad value error"
        AudioRecord.ERROR_DEAD_OBJECT -> "AudioRecord returns a dead object error"
        else -> "Unknown audio record error: $audioRecordError"
    }

    companion object {
        private val format = MediaFormat().apply {
            setString(
                MediaFormat.KEY_MIME,
                MediaFormat.MIMETYPE_AUDIO_RAW
            )
        }
    }
}