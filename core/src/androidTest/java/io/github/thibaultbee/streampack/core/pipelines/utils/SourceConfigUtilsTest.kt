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

import android.util.Size
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SourceConfigUtilsTest {

    @Test
    fun videoSourceConfigFromEmpty() {
        try {
            SourceConfigUtils.buildVideoSourceConfig(emptySet())
            fail("Video source configs must not be empty")
        } catch (_: Throwable) {
        }
    }

    @Test
    fun buildVideoSourceConfigWithSimple() {
        // Given
        val videoSourceConfigs = setOf(
            VideoSourceConfig(
                resolution = Size(1280, 720),
                fps = 30
            ),
            VideoSourceConfig(resolution = Size(1280, 720), fps = 30)
        )

        // When
        val videoSourceConfig = SourceConfigUtils.buildVideoSourceConfig(videoSourceConfigs)

        // Then
        assertEquals(1280, videoSourceConfig.resolution.width)
        assertEquals(720, videoSourceConfig.resolution.height)
        assertEquals(30, videoSourceConfig.fps)
    }

    @Test
    fun buildVideoSourceConfigWithDifferentResolution() {
        // Given
        val videoSourceConfigs = setOf(
            VideoSourceConfig(resolution = Size(1280, 720)),
            VideoSourceConfig(resolution = Size(1920, 1080))
        )

        // When
        val videoSourceConfig = SourceConfigUtils.buildVideoSourceConfig(videoSourceConfigs)

        // Then
        assertEquals(1920, videoSourceConfig.resolution.width)
        assertEquals(1080, videoSourceConfig.resolution.height)
    }

    @Test
    fun videoSourceConfigWithDifferentFps() {
        // Given
        val videoSourceConfigs = setOf(
            VideoSourceConfig(fps = 30),
            VideoSourceConfig(fps = 25)
        )

        // When
        try {
            SourceConfigUtils.buildVideoSourceConfig(videoSourceConfigs)
            fail("All video source configs must have the same fps")
        } catch (e: IllegalArgumentException) {
            assertEquals("All video source configs must have the same fps but [30, 25]", e.message)
        }
    }
}