/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.processing.audio

/**
 * Represents audio level data for one or two channels.
 * For mono, only left channel values are used.
 * For stereo, both left and right channel values are provided.
 */
data class AudioLevelData(
    val channelCount: Int,
    val rmsLeft: Float,
    val peakLeft: Float,
    val rmsRight: Float = 0f,
    val peakRight: Float = 0f
) {
    val isStereo: Boolean get() = channelCount >= 2
}

/**
 * Callback for audio level updates.
 * 
 * @param levels Audio level data containing RMS and peak for each channel
 */
typealias AudioLevelCallback = (levels: AudioLevelData) -> Unit

/**
 * Public interface for audio frame processor.
 */
interface IAudioFrameProcessor : MutableList<IAudioEffect> {
    /**
     * Whether the processor is muted.
     */
    var isMuted: Boolean
    
    /**
     * Number of audio channels (1 for mono, 2 for stereo).
     * Should be set before streaming starts.
     */
    var channelCount: Int
    
    /**
     * Callback for audio level updates.
     * Called for each audio frame with RMS and peak values (0.0 to 1.0 linear scale).
     * Set to null to disable audio level monitoring.
     */
    var audioLevelCallback: AudioLevelCallback?
}