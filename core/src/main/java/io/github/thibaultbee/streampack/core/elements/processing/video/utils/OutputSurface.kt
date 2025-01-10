/*
 * Copyright 2024 The Android Open Source Project
 * Copyright 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.opengl.EGLSurface

/**
 * Wrapper for output [EGLSurface] in [io.github.thibaultbee.streampack.core.elements.processing.video.OpenGlRenderer].
 */

data class OutputSurface(
    /**
     * Gets [EGLSurface].
     */
    val eglSurface: EGLSurface,

    /**
     * Gets [EGLSurface] width.
     */
    val width: Int,

    /**
     * Gets [EGLSurface] height.
     */
    val height: Int
) {

    companion object {
        /**
         * Creates [OutputSurface].
         */
        fun of(eglSurface: EGLSurface, width: Int, height: Int): OutputSurface {
            return OutputSurface(eglSurface, width, height)
        }
    }
}