/*
 * Copyright 2026 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.graphics.RectF
import io.github.thibaultbee.streampack.core.elements.utils.Position
import io.github.thibaultbee.streampack.core.elements.utils.ScaleMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransformUtilsTest {

    @Test
    fun `calculateViewfinder returns target when aspect ratios match`() {
        val source = RectF(0f, 0f, 1920f, 1080f)
        val target = RectF(0f, 0f, 1280f, 720f)

        val resultCrop = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.CENTER)
        assertEquals(target, resultCrop)

        val resultFit = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.CENTER)
        assertEquals(target, resultFit)
    }

    @Test
    fun `calculateViewfinder CROP when target is wider than source`() {
        // target (source video) is 1920x1080 (16:9), source (output surface) is 1080x1080 (1:1)
        val source = RectF(0f, 0f, 1080f, 1080f)
        val target = RectF(0f, 0f, 1920f, 1080f)

        // Center: width should be 1080, centered horizontally -> left = 420, right = 1500
        val center = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.CENTER)
        assertEquals(RectF(420f, 0f, 1500f, 1080f), center)

        // Start: aligned to left -> left = 0, right = 1080
        val start = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.START)
        assertEquals(RectF(0f, 0f, 1080f, 1080f), start)

        // End: aligned to right -> left = 840, right = 1920
        val end = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.END)
        assertEquals(RectF(840f, 0f, 1920f, 1080f), end)
    }

    @Test
    fun `calculateViewfinder CROP when target is taller than source`() {
        // target is 1080x1920 (9:16), source is 1080x1080 (1:1)
        val source = RectF(0f, 0f, 1080f, 1080f)
        val target = RectF(0f, 0f, 1080f, 1920f)

        // Center: height should be 1080, centered vertically -> top = 420, bottom = 1500
        val center = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.CENTER)
        assertEquals(RectF(0f, 420f, 1080f, 1500f), center)

        // Start: aligned to top -> top = 0, bottom = 1080
        val start = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.START)
        assertEquals(RectF(0f, 0f, 1080f, 1080f), start)

        // End: aligned to bottom -> top = 840, bottom = 1920
        val end = TransformUtils.calculateViewfinder(source, target, ScaleMode.CROP, Position.END)
        assertEquals(RectF(0f, 840f, 1080f, 1920f), end)
    }

    @Test
    fun `calculateViewfinder FIT when target is wider than source`() {
        // target is 1920x1080 (16:9), source is 1080x1080 (1:1)
        val source = RectF(0f, 0f, 1080f, 1080f)
        val target = RectF(0f, 0f, 1920f, 1080f)

        // In FIT, height expands to 1920 so 16:9 fits in 1:1 with letterbox
        // Center: top = -420, bottom = 1500
        val center = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.CENTER)
        assertEquals(RectF(0f, -420f, 1920f, 1500f), center)

        // Start: aligned to start (top) -> top = 0, bottom = 1920
        val start = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.START)
        assertEquals(RectF(0f, 0f, 1920f, 1920f), start)

        // End: aligned to end (bottom) -> top = -840, bottom = 1080
        val end = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.END)
        assertEquals(RectF(0f, -840f, 1920f, 1080f), end)
    }

    @Test
    fun `calculateViewfinder FIT when target is taller than source`() {
        // target is 1080x1920 (9:16), source is 1080x1080 (1:1)
        val source = RectF(0f, 0f, 1080f, 1080f)
        val target = RectF(0f, 0f, 1080f, 1920f)

        // In FIT, width expands to 1920 so 9:16 fits in 1:1 with pillarbox
        // Center: left = -420, right = 1500
        val center = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.CENTER)
        assertEquals(RectF(-420f, 0f, 1500f, 1920f), center)

        // Start: aligned to start (left) -> left = 0, right = 1920
        val start = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.START)
        assertEquals(RectF(0f, 0f, 1920f, 1920f), start)

        // End: aligned to end (right) -> left = -840, right = 1080
        val end = TransformUtils.calculateViewfinder(source, target, ScaleMode.FIT, Position.END)
        assertEquals(RectF(-840f, 0f, 1080f, 1920f), end)
    }

    @Test
    fun `getRectToRect maps correctly with rotation and mirroring`() {
        val source = RectF(0f, 0f, 1920f, 1080f)
        val target = RectF(0f, 0f, 1080f, 1920f)

        val matrix0 = TransformUtils.getRectToRect(source, source, 0, false)
        val mapped0 = RectF()
        matrix0.mapRect(mapped0, source)
        assertEquals(source, mapped0)

        val matrix90 = TransformUtils.getRectToRect(source, target, 90, false)
        val mapped90 = RectF()
        matrix90.mapRect(mapped90, source)
        assertEquals(target, mapped90)
    }
}
