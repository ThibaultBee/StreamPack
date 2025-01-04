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
package io.github.thibaultbee.streampack.core.pipelines.utils

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isCompatibleWith
import io.github.thibaultbee.streampack.core.elements.utils.extensions.sourceConfig

object SourceConfigUtils {
    fun buildAudioSourceConfig(audioCodecConfigs: Set<AudioCodecConfig>): AudioSourceConfig {
        require(audioCodecConfigs.isNotEmpty()) { "No audio codec config provided" }
        val firstSourceConfig = audioCodecConfigs.first().sourceConfig
        require(audioCodecConfigs.all { it.isCompatibleWith(firstSourceConfig) }) { "All audio codec configs must be compatible to $firstSourceConfig" }
        return firstSourceConfig
    }

    fun buildVideoSourceConfig(videoCodecConfigs: Set<VideoCodecConfig>): VideoSourceConfig {
        require(videoCodecConfigs.isNotEmpty()) { "No video codec config provided" }
        val maxResolution =
            videoCodecConfigs.map { it.resolution }.maxWith(compareBy({ it.width }, { it.height }))
        val fps = videoCodecConfigs.first().fps
        require(videoCodecConfigs.all { it.fps == fps }) { "All video codec configs must have the same fps" }
        val dynamicRangeProfile = videoCodecConfigs.first().dynamicRangeProfile
        require(videoCodecConfigs.all { it.dynamicRangeProfile == dynamicRangeProfile }) { "All video codec configs must have the same dynamic range profile" }
        return VideoSourceConfig(maxResolution, fps, dynamicRangeProfile)
    }
}