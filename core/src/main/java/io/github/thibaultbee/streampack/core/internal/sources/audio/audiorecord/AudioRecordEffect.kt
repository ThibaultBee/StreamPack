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
package io.github.thibaultbee.streampack.core.internal.sources.audio.audiorecord

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.util.UUID

/**
 * Audio effect interface
 */
internal class AudioRecordEffect {
    internal interface IFactory {
        fun build(audioSessionId: Int): AudioEffect?
    }

    companion object {
        val supportedEffects: List<UUID> = getSupportedEffectsInternal()

        private fun getSupportedEffectsInternal(): List<UUID> {
            val descriptors = AudioEffect.queryEffects()
            return descriptors.map { it.type }.filter { isValidUUID(it) }
        }

        val availableEffects: List<UUID>
            get() = supportedEffects.filter { isEffectAvailable(it) }

        fun isEffectAvailable(effectType: UUID): Boolean {
            if (!isValidUUID(effectType)) {
                return false
            }
            return when (effectType) {
                AudioEffect.EFFECT_TYPE_AEC -> AcousticEchoCanceler.isAvailable()
                AudioEffect.EFFECT_TYPE_AGC -> AutomaticGainControl.isAvailable()
                AudioEffect.EFFECT_TYPE_NS -> NoiseSuppressor.isAvailable()
                else -> AudioEffect.queryEffects()
                    .any { it.type == effectType }
            }
        }

        /**
         * Whether the effect is supported by the device
         *
         * @param uuid Effect type
         * @return true if effect is supported
         */
        fun isValidUUID(uuid: UUID): Boolean {
            return uuid == AudioEffect.EFFECT_TYPE_AEC ||
                    uuid == AudioEffect.EFFECT_TYPE_AGC ||
                    // uuid == AudioEffect.EFFECT_TYPE_BASS_BOOST || // Not for AudioRecord
                    // ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) &&
                    //        uuid == AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING) || // Not for AudioRecord
                    // uuid == AudioEffect.EFFECT_TYPE_ENV_REVERB || // Not for AudioRecord
                    // uuid == AudioEffect.EFFECT_TYPE_EQUALIZER || // Not for AudioRecord
                    //((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
                    //        uuid == AudioEffect.EFFECT_TYPE_HAPTIC_GENERATOR) || // Not for AudioRecord
                    // uuid == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER || // Not for AudioRecord
                    uuid == AudioEffect.EFFECT_TYPE_NS
            // uuid == AudioEffect.EFFECT_TYPE_PRESET_REVERB || // Not for AudioRecord
            // uuid == AudioEffect.EFFECT_TYPE_VIRTUALIZER // Not for AudioRecord
            // uuid == AudioEffect.EFFECT_TYPE_VISUALIZER // Not for AudioRecord
        }
    }

    internal abstract class Factory<T : AudioEffect> : IFactory {
        protected abstract val name: String

        protected abstract fun isAvailable(): Boolean
        protected abstract fun create(audioSessionId: Int): T?

        override fun build(audioSessionId: Int): T {
            if (!isAvailable()) {
                throw IllegalStateException("$name is not available")
            }

            val audioEffect = try {
                create(audioSessionId)
            } catch (t: Throwable) {
                throw Exception("Failed to create $name", t)
            }

            requireNotNull(audioEffect) {
                "Failed to create $name"
            }

            return audioEffect
        }

        companion object {
            fun getFactoryForEffectType(effectType: UUID): Factory<*> {
                return when (effectType) {
                    AudioEffect.EFFECT_TYPE_AEC -> AcousticEchoCancelerFactory()
                    AudioEffect.EFFECT_TYPE_AGC -> AutomaticGainControlFactory()
                    AudioEffect.EFFECT_TYPE_NS -> NoiseSuppressorFactory()
                    else -> throw IllegalArgumentException("Unknown effect type: $effectType")
                }
            }
        }
    }

    internal class AcousticEchoCancelerFactory : Factory<AcousticEchoCanceler>() {
        override val name: String = "Acoustic echo canceler"

        override fun isAvailable(): Boolean = AcousticEchoCanceler.isAvailable()

        override fun create(audioSessionId: Int): AcousticEchoCanceler? =
            AcousticEchoCanceler.create(audioSessionId)
    }

    internal class AutomaticGainControlFactory : Factory<AutomaticGainControl>() {
        override val name: String = "Automatic gain control"

        override fun isAvailable(): Boolean = AutomaticGainControl.isAvailable()

        override fun create(audioSessionId: Int): AutomaticGainControl? =
            AutomaticGainControl.create(audioSessionId)
    }

    internal class NoiseSuppressorFactory : Factory<NoiseSuppressor>() {
        override val name: String = "Noise suppressor"

        override fun isAvailable(): Boolean = NoiseSuppressor.isAvailable()

        override fun create(audioSessionId: Int): NoiseSuppressor? =
            NoiseSuppressor.create(audioSessionId)
    }
}
