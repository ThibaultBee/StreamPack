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
package io.github.thibaultbee.streampack.core.elements.utils

/**
 * The scale mode to use when scaling video frames into a target surface or view.
 */
enum class ScaleMode(val value: Int) {
    /**
     * Scales the video preserving aspect ratio so that it fills the entire target surface.
     * Content that does not fit within the target bounds is cropped.
     */
    CROP(0),

    /**
     * Scales the video preserving aspect ratio so that it fits entirely within the target surface.
     * Letterbox or pillarbox bars may be added if the aspect ratios differ.
     */
    FIT(1);

    companion object {
        @Deprecated("Use CROP instead", ReplaceWith("CROP"))
        val FILL = CROP

        fun entryOf(value: Int) = entries.first { it.value == value }
    }
}
