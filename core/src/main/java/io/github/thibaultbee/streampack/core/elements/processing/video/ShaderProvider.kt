/*
 * Copyright 2022 The Android Open Source Project
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
package io.github.thibaultbee.streampack.core.elements.processing.video

/**
 * A provider that supplies OpenGL shader code.
 */
interface ShaderProvider {
    /**
     * Creates the fragment shader code with the given variable names.
     *
     *
     * The provider must use the variable names to construct the shader code, or it will fail
     * to create the OpenGL program when the provider is used. For example:
     * <pre>`#extension GL_OES_EGL_image_external : require
     * precision mediump float;
     * uniform samplerExternalOES {$samplerVarName};
     * varying vec2 {$fragCoordsVarName};
     * void main() {
     * vec4 sampleColor = texture2D({$samplerVarName}, {$fragCoordsVarName});
     * gl_FragColor = vec4(
     * sampleColor.r * 0.5 + sampleColor.g * 0.8 + sampleColor.b * 0.3,
     * sampleColor.r * 0.4 + sampleColor.g * 0.7 + sampleColor.b * 0.2,
     * sampleColor.r * 0.3 + sampleColor.g * 0.5 + sampleColor.b * 0.1,
     * 1.0);
     * }
    `</pre> *
     *
     * @param samplerVarName    the variable name of the samplerExternalOES.
     * @param fragCoordsVarName the variable name of the fragment coordinates.
     * @return the shader code. Return null to use the default shader.
     */
    fun createFragmentShader(
        samplerVarName: String,
        fragCoordsVarName: String
    ): String? {
        return null
    }
}