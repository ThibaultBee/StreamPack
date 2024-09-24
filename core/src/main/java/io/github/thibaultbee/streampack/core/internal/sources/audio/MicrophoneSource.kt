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
package io.github.thibaultbee.streampack.core.internal.sources.audio

import android.Manifest
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.sources.IFrameSource
import io.github.thibaultbee.streampack.core.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer

class MicrophoneSource : IAudioSource, IFrameSource<AudioConfig> {
    private var audioRecord: AudioRecord? = null

    private var processor: EffectProcessor? = null

    private var mutedByteArray: ByteArray? = null
    override var isMuted: Boolean = false

    private val isRunning: Boolean
        get() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

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
            processor = EffectProcessor(
                config.enableEchoCanceler,
                config.enableNoiseSuppressor,
                it.audioSessionId
            )
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalArgumentException("Failed to initialized AudioRecord")
        }
    }

    override fun startStream() {
        audioRecord?.startRecording() ?: throw IllegalStateException("AudioRecorder not initialized")
    }

    override fun stopStream() {
        if (!isRunning) {
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

        processor?.release()
        processor = null
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
                    Frame(
                        buffer,
                        getTimestamp(it),
                        format = format
                    )
                } else {
                    Frame(
                        buffer,
                        getTimestamp(it),
                        format = format
                    )
                }
            } else {
                throw IllegalArgumentException(audioRecordErrorToString(length))
            }
        } ?: throw IllegalStateException("AudioRecorder not initialized")
    }

    private fun audioRecordErrorToString(audioRecordError: Int) = when (audioRecordError) {
        AudioRecord.ERROR_INVALID_OPERATION -> "AudioRecord returns an invalid operation error"
        AudioRecord.ERROR_BAD_VALUE -> "AudioRecord returns a bad value error"
        AudioRecord.ERROR_DEAD_OBJECT -> "AudioRecord returns a dead object error"
        else -> "Unknown audio record error: $audioRecordError"
    }

    companion object {
        private const val TAG = "AudioSource"

        private val format = MediaFormat().apply {
            setString(
                MediaFormat.KEY_MIME,
                MediaFormat.MIMETYPE_AUDIO_RAW
            )
        }
    }

    private class EffectProcessor(
        enableAcousticEchoCanceler: Boolean,
        enableNoiseSuppressor: Boolean,
        audioSessionId: Int
    ) {
        private val acousticEchoCanceler =
            if (enableAcousticEchoCanceler) initAcousticEchoCanceler(audioSessionId) else null

        private val noiseSuppressor =
            if (enableNoiseSuppressor) initNoiseSuppressor(audioSessionId) else null


        fun release() {
            acousticEchoCanceler?.release()
            noiseSuppressor?.release()
        }

        companion object {
            private fun initNoiseSuppressor(audioSessionId: Int): NoiseSuppressor? {
                if (!NoiseSuppressor.isAvailable()) {
                    Logger.w(TAG, "Noise suppressor is not available")
                    return null
                }

                val noiseSuppressor = try {
                    NoiseSuppressor.create(audioSessionId)
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to create noise suppressor", t)
                    return null
                }

                if (noiseSuppressor == null) {
                    Logger.w(TAG, "Failed to create noise suppressor")
                    return null
                }

                val result = noiseSuppressor.setEnabled(true)
                if (result != NoiseSuppressor.SUCCESS) {
                    noiseSuppressor.release()
                    Logger.w(TAG, "Failed to enable noise suppressor")
                    return null
                }

                return noiseSuppressor
            }

            private fun initAcousticEchoCanceler(audioSessionId: Int): AcousticEchoCanceler? {
                if (!AcousticEchoCanceler.isAvailable()) {
                    Logger.w(TAG, "Acoustic echo canceler is not available")
                    return null
                }

                val acousticEchoCanceler = try {
                    AcousticEchoCanceler.create(audioSessionId)
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to create acoustic echo canceler", t)
                    return null
                }

                if (acousticEchoCanceler == null) {
                    Logger.w(TAG, "Failed to create acoustic echo canceler")
                    return null
                }

                val result = acousticEchoCanceler.setEnabled(true)
                if (result != AcousticEchoCanceler.SUCCESS) {
                    acousticEchoCanceler.release()
                    Logger.w(TAG, "Failed to enable acoustic echo canceler")
                    return null
                }

                return acousticEchoCanceler
            }
        }
    }
}