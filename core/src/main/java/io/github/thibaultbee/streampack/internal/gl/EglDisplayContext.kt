/*
 * Copyright 2018 Google Inc. All rights reserved.
 * Copyright 2021 Thibault B.
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
package io.github.thibaultbee.streampack.internal.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import java.util.Objects

class EglDisplayContext(useHighBitDepth: Boolean = false) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private val configs = arrayOfNulls<EGLConfig>(1)

    val display: EGLDisplay
        get() = eglDisplay

    val context: EGLContext
        get() = eglContext
    val config: EGLConfig?
        get() = configs[0]

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }

        // Configure EGL for recordable and OpenGL ES 2.0.  We want enough RGB bits
        // to minimize artifacts from possible YUV conversion.
        val eglColorSize = if (useHighBitDepth) 10 else 8
        val eglAlphaSize = if (useHighBitDepth) 2 else 0
        val recordable = if (useHighBitDepth) 0 else 1
        var attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, eglColorSize,
            EGL14.EGL_GREEN_SIZE, eglColorSize,
            EGL14.EGL_BLUE_SIZE, eglColorSize,
            EGL14.EGL_ALPHA_SIZE, eglAlphaSize,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, recordable,
            EGL14.EGL_NONE
        )
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }

        // Configure context for OpenGL ES 2.0.
        attribList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attribList, 0
        )
        GlUtils.checkEglError("eglCreateContext")

    }

    /**
     * Discard all resources held by this class, notably the EGL context. Also releases the
     * Surface that was passed to our constructor.
     */
    fun release() {
        if (!Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
    }

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                context)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun makeUnCurrent() {
        if (!EGL14.eglMakeCurrent(
                display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        ) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }
}