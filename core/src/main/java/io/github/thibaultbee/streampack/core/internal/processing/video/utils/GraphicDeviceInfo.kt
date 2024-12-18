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
package io.github.thibaultbee.streampack.core.internal.processing.video.utils

import io.github.thibaultbee.streampack.core.internal.processing.video.utils.GLUtils.VERSION_UNKNOWN

/**
 * Information about an initialized graphics device.
 *
 *
 * This information can be used to determine which version or extensions of OpenGL and EGL
 * are supported on the device to ensure the attached output surface will have expected
 * characteristics.
 */
class GraphicDeviceInfo  // Should not be instantiated directly
private constructor(
    /**
     * Returns the OpenGL version this graphics device has been initialized to.
     *
     *
     * The version is in the form &lt;major&gt;.&lt;minor&gt;.
     *
     *
     * Returns [GLUtils.VERSION_UNKNOWN] if version information can't be
     * retrieved.
     */
    val glVersion: String = VERSION_UNKNOWN,

    /**
     * Returns the EGL version this graphics device has been initialized to.
     *
     *
     * The version is in the form &lt;major&gt;.&lt;minor&gt;.
     *
     *
     * Returns [GLUtils.VERSION_UNKNOWN] if version information can't be
     * retrieved.
     */
    val eglVersion: String = VERSION_UNKNOWN,

    /**
     * Returns a space separated list of OpenGL extensions or an empty string if extensions
     * could not be retrieved.
     */
    val glExtensions: String = "",

    /**
     * Returns a space separated list of EGL extensions or an empty string if extensions
     * could not be retrieved.
     */
    val eglExtensions: String = ""
) {
    class Builder {
        private var glVersion: String = VERSION_UNKNOWN
        private var eglVersion: String = VERSION_UNKNOWN
        private var glExtensions: String = ""
        private var eglExtensions: String = ""

        fun setGlVersion(glVersion: String) = apply { this.glVersion = glVersion }
        fun setEglVersion(eglVersion: String) = apply { this.eglVersion = eglVersion }
        fun setGlExtensions(glExtensions: String) = apply { this.glExtensions = glExtensions }
        fun setEglExtensions(eglExtensions: String) = apply { this.eglExtensions = eglExtensions }

        fun build() = GraphicDeviceInfo(glVersion, eglVersion, glExtensions, eglExtensions)
    }
}