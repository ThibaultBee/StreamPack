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

import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isCompatibleWith

object SourceConfigUtils {
    fun buildAudioSourceConfig(audioSourceConfigs: Set<AudioSourceConfig>): AudioSourceConfig {
        require(audioSourceConfigs.isNotEmpty()) { "No audio source config provided" }
        val firstSourceConfig = audioSourceConfigs.first()
        require(audioSourceConfigs.all { it.isCompatibleWith(firstSourceConfig) }) { "All audio source configs must be compatible to $firstSourceConfig" }
        return firstSourceConfig
    }

    fun buildVideoSourceConfig(videoSourceConfigs: Set<VideoSourceConfig>): VideoSourceConfig {
        require(videoSourceConfigs.isNotEmpty()) { "No video source config provided" }
        val maxResolution =
            videoSourceConfigs.map { it.resolution }.maxWith(compareBy({ it.width }, { it.height }))
        val fps = videoSourceConfigs.map { it.fps }.distinct()
        require(fps.distinct().size == 1) { "All video source configs must have the same fps but $fps" }
        val dynamicRangeProfiles = videoSourceConfigs.map { it.dynamicRangeProfile }.distinct()
        require(dynamicRangeProfiles.size == 1) { "All video source configs must have the same dynamic range profile but $dynamicRangeProfiles" }
        return VideoSourceConfig(maxResolution, fps.first(), dynamicRangeProfiles.first())
    }
}