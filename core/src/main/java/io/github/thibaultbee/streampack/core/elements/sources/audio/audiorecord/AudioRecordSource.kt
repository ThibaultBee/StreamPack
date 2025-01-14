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
package io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord

import android.Manifest
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaFormat
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.AudioRecordEffect.Companion.isValidUUID
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.AudioRecordEffect.Factory.Companion.getFactoryForEffectType
import io.github.thibaultbee.streampack.core.elements.utils.TimeUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.type
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer
import java.util.UUID

/**
 * The [AudioRecordSource] class is an implementation of [IAudioSourceInternal] that captures audio
 * from [AudioRecord].
 */
sealed class AudioRecordSource : IAudioSourceInternal, IAudioRecordSource {
    private var audioRecord: AudioRecord? = null

    private var processor: EffectProcessor? = null
    private var pendingAudioEffects = mutableListOf<UUID>()

    private var mutedByteArray: ByteArray? = null
    override var isMuted: Boolean = false

    private val isRunning: Boolean
        get() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    protected abstract fun buildAudioRecord(
        config: AudioSourceConfig,
        bufferSize: Int
    ): AudioRecord

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(config: AudioSourceConfig) {
        /**
         * [configure] might be called multiple times.
         * If audio source is already running, we need to prevent reconfiguration.
         */
        audioRecord?.let {
            if (it.state == AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("Audio source is already running")
            } else {
                release()
            }
        }

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

        audioRecord = buildAudioRecord(config, bufferSize).also {
            val previousEffects = processor?.getAll() ?: emptyList()
            processor?.clear()

            // Add effects
            processor = EffectProcessor(it.audioSessionId).apply {
                (previousEffects + pendingAudioEffects).forEach { effectType ->
                    try {
                        add(effectType)
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Failed to add effect: $effectType: ${t.message}")
                    }
                }
                pendingAudioEffects.clear()
            }

            if (it.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalArgumentException("Failed to initialized audio source with config: $config")
            }
        }
    }

    override fun startStream() {
        if (isRunning) {
            Logger.d(TAG, "Already running")
            return
        }
        val audioRecord = requireNotNull(audioRecord)

        processor?.setEnabled(true)

        audioRecord.startRecording()
    }

    override fun stopStream() {
        if (!isRunning) {
            Logger.d(TAG, "Not running")
            return
        }

        // Stop audio record
        audioRecord?.stop()

        processor?.setEnabled(false)
    }

    override fun release() {
        mutedByteArray = null

        processor?.clear()
        processor = null

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

    override fun getAudioFrame(buffer: ByteBuffer): Frame {
        val audioRecord = requireNotNull(audioRecord)
        val length = audioRecord.read(buffer, buffer.remaining())
        if (length >= 0) {
            return if (isMuted) {
                if (length != mutedByteArray?.size) {
                    mutedByteArray = ByteArray(length)
                }
                buffer.put(mutedByteArray!!, 0, length)
                buffer.clear()
                Frame(
                    buffer,
                    getTimestamp(audioRecord),
                    format = rawFormat
                )
            } else {
                Frame(
                    buffer,
                    getTimestamp(audioRecord),
                    format = rawFormat
                )
            }
        } else {
            throw IllegalArgumentException(audioRecordErrorToString(length))
        }
    }

    private fun audioRecordErrorToString(audioRecordError: Int) = when (audioRecordError) {
        AudioRecord.ERROR_INVALID_OPERATION -> "AudioRecord returns an invalid operation error"
        AudioRecord.ERROR_BAD_VALUE -> "AudioRecord returns a bad value error"
        AudioRecord.ERROR_DEAD_OBJECT -> "AudioRecord returns a dead object error"
        else -> "Unknown audio record error: $audioRecordError"
    }

    /**
     * Adds and enables an effect to the audio source.
     *
     * Get supported effects with [getSupportedEffectTypes].
     */
    override fun addEffect(effectType: UUID): Boolean {
        require(isValidUUID(effectType)) { "Unsupported effect type: $effectType" }
        require(isEffectAvailable(effectType)) { "Effect $effectType is not available" }

        val processor = processor
        return if (processor == null) {
            pendingAudioEffects.add(effectType)
            false
        } else {
            try {
                processor.add(effectType)
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to add effect: $effectType: ${t.message}")
                false
            }
        }
    }

    /**
     * Removes an effect from the audio source.
     */
    override fun removeEffect(effectType: UUID) {
        val processor = processor
        if (processor == null) {
            pendingAudioEffects.remove(effectType)
            return
        } else {
            processor.remove(effectType)
        }
    }

    companion object {
        private const val TAG = "AudioSource"

        private val rawFormat = MediaFormat().apply {
            setString(
                MediaFormat.KEY_MIME,
                MediaFormat.MIMETYPE_AUDIO_RAW
            )
        }

        /**
         * Get available effects.
         *
         * @return [List] of supported effects.
         * @see AudioEffect
         */
        val availableEffect: List<UUID>
            get() = AudioRecordEffect.availableEffects

        /**
         * Whether the effect is available.
         *
         * @param effectType Effect type
         * @return true if effect is available
         */
        fun isEffectAvailable(effectType: UUID): Boolean {
            return AudioRecordEffect.isEffectAvailable(effectType)
        }
    }


    private class EffectProcessor(private val audioSessionId: Int) {
        private val audioEffects: MutableSet<AudioEffect> = mutableSetOf()

        init {
            require(audioSessionId >= 0) { "Invalid audio session ID: $audioSessionId" }
        }

        fun getAll(): List<UUID> {
            return audioEffects.map { it.type }
        }

        fun add(effectType: UUID): Boolean {
            require(isValidUUID(effectType)) { "Unsupported effect type: $effectType" }

            val previousEffect = audioEffects.firstOrNull { it.type == effectType }
            if (previousEffect != null) {
                Logger.w(TAG, "Effect ${previousEffect.descriptor.name} already enabled")
                return false
            }

            val factory = getFactoryForEffectType(effectType)
            factory.build(audioSessionId).let {
                audioEffects.add(it)
                return true
            }
        }

        fun setEnabled(enabled: Boolean) {
            audioEffects.forEach { it.enabled = enabled }
        }

        fun remove(effectType: UUID) {
            require(isValidUUID(effectType)) { "Unknown effect type: $effectType" }

            val effect = audioEffects.firstOrNull { it.descriptor.type == effectType }
            if (effect != null) {
                effect.release()
                audioEffects.remove(effect)
            }
        }

        fun release() {
            audioEffects.forEach { it.release() }
        }

        fun clear() {
            release()
            audioEffects.clear()
        }

        companion object {
            private const val TAG = "EffectProcessor"
        }
    }
}