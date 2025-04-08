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

import android.graphics.Rect
import android.opengl.EGLSurface

/**
 * Creates an [OutputSurface] with the given [EGLSurface] and viewport is full screen.
 */
fun OutputSurface(eglSurface: EGLSurface, width: Int, height: Int) =
    OutputSurface(
        eglSurface,
        Rect(0, 0, width, height)
    )

/**
 * Wrapper for output [EGLSurface] in [OpenGlRenderer].
 */

data class OutputSurface(
    /**
     * Gets [EGLSurface].
     */
    val eglSurface: EGLSurface,

    /**
     * Gets the [Rect] that defines the viewport.
     */
    val viewPortRect: Rect,
)